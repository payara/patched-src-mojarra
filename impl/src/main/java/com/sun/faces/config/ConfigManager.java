/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
// Portions Copyright [2018] Payara Foundation and/or affiliates
package com.sun.faces.config;

import static com.sun.faces.RIConstants.FACES_PREFIX;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.DisableFaceletJSFViewHandler;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.DisableFaceletJSFViewHandlerDeprecated;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.EnableParallelInit;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.EnableThreading;
import static com.sun.faces.config.WebConfiguration.BooleanWebContextInitParameter.ValidateFacesConfigFiles;
import static com.sun.faces.config.manager.Documents.getProgrammaticDocuments;
import static com.sun.faces.config.manager.Documents.getXMLDocuments;
import static com.sun.faces.config.manager.Documents.mergeDocuments;
import static com.sun.faces.config.manager.Documents.sortDocuments;
import static com.sun.faces.spi.ConfigurationResourceProviderFactory.createProviders;
import static com.sun.faces.spi.ConfigurationResourceProviderFactory.ProviderType.FaceletConfig;
import static com.sun.faces.spi.ConfigurationResourceProviderFactory.ProviderType.FacesConfig;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static java.util.Collections.unmodifiableList;
import static java.util.logging.Level.FINE;
import static java.util.stream.StreamSupport.stream;

import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.FutureTask;
import java.util.logging.Logger;

import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.el.ExpressionFactory;
import javax.faces.FacesException;
import javax.faces.FactoryFinder;
import javax.faces.application.Application;
import javax.faces.application.ApplicationConfigurationPopulator;
import javax.faces.component.UIViewRoot;
import javax.faces.context.FacesContext;
import javax.faces.event.PostConstructApplicationEvent;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletContext;

import com.sun.faces.config.configprovider.MetaInfFaceletTaglibraryConfigProvider;
import com.sun.faces.config.configprovider.MetaInfFacesConfigResourceProvider;
import com.sun.faces.config.configprovider.WebAppFlowConfigResourceProvider;
import com.sun.faces.config.configprovider.WebFaceletTaglibResourceProvider;
import com.sun.faces.config.configprovider.WebFacesConfigResourceProvider;
import com.sun.faces.config.manager.DbfFactory;
import com.sun.faces.config.manager.FacesConfigInfo;
import com.sun.faces.config.manager.documents.DocumentInfo;
import com.sun.faces.config.manager.tasks.FindAnnotatedConfigClasses;
import com.sun.faces.config.manager.tasks.ProvideMetadataToAnnotationScanTask;
import com.sun.faces.config.processor.ApplicationConfigProcessor;
import com.sun.faces.config.processor.BehaviorConfigProcessor;
import com.sun.faces.config.processor.ComponentConfigProcessor;
import com.sun.faces.config.processor.ConfigProcessor;
import com.sun.faces.config.processor.ConverterConfigProcessor;
import com.sun.faces.config.processor.FaceletTaglibConfigProcessor;
import com.sun.faces.config.processor.FacesConfigExtensionProcessor;
import com.sun.faces.config.processor.FacesFlowDefinitionConfigProcessor;
import com.sun.faces.config.processor.FactoryConfigProcessor;
import com.sun.faces.config.processor.LifecycleConfigProcessor;
import com.sun.faces.config.processor.ManagedBeanConfigProcessor;
import com.sun.faces.config.processor.NavigationConfigProcessor;
import com.sun.faces.config.processor.ProtectedViewsConfigProcessor;
import com.sun.faces.config.processor.RenderKitConfigProcessor;
import com.sun.faces.config.processor.ResourceLibraryContractsConfigProcessor;
import com.sun.faces.config.processor.ValidatorConfigProcessor;
import com.sun.faces.el.ELContextImpl;
import com.sun.faces.el.ELUtils;
import com.sun.faces.spi.ConfigurationResourceProvider;
import com.sun.faces.spi.ConfigurationResourceProviderFactory;
import com.sun.faces.spi.HighAvailabilityEnabler;
import com.sun.faces.spi.InjectionProvider;
import com.sun.faces.spi.InjectionProviderFactory;
import com.sun.faces.spi.ThreadContext;
import com.sun.faces.util.FacesLogger;

/**
 * <p>
 *  This class manages the initialization of each web application that uses
 *  JSF.
 * </p>
 */
public class ConfigManager {
    
    private static final Logger LOGGER = FacesLogger.CONFIG.getLogger();
    
    /**
     * The initialization time FacesContext scoped key under which the
     * InjectionProvider is stored.
     */
    public static final String INJECTION_PROVIDER_KEY = ConfigManager.class.getName() + "_INJECTION_PROVIDER_TASK";


    /**
     * <p>
     *  The <code>ConfigManager</code> will multithread the calls to the
     *  <code>ConfigurationResourceProvider</code>s as well as any calls
     *  to parse a resources into a DOM.  By default, we'll use only 5 threads
     *  per web application.
     * </p>
     */
    private static final int NUMBER_OF_TASK_THREADS = 5;

    private static final String CONFIG_MANAGER_INSTANCE_KEY = FACES_PREFIX + "CONFIG_MANAGER_KEY";

    /**
     * The application-scoped key under which the Future responsible for annotation
     * scanning is associated with.
     */
    private static final String ANNOTATIONS_SCAN_TASK_KEY = ConfigManager.class.getName() + "_ANNOTATION_SCAN_TASK";
 
    
    /**
     * <p>
     *   Contains each <code>ServletContext</code> that we've initialized.
     *   The <code>ServletContext</code> will be removed when the application
     *   is destroyed.
     * </p>
     */
    private List<ServletContext> initializedContexts = new CopyOnWriteArrayList<>();

    private final List<ConfigProcessor> configProcessors = unmodifiableList(asList(
            new FactoryConfigProcessor(),
            new LifecycleConfigProcessor(),
            new ApplicationConfigProcessor(),
            new ComponentConfigProcessor(),
            new ConverterConfigProcessor(),
            new ValidatorConfigProcessor(),
            new ManagedBeanConfigProcessor(),
            new RenderKitConfigProcessor(),
            new NavigationConfigProcessor(),
            new BehaviorConfigProcessor(),
            new FacesConfigExtensionProcessor(),
            new ProtectedViewsConfigProcessor(),
            new FacesFlowDefinitionConfigProcessor(),
            new ResourceLibraryContractsConfigProcessor()));
    
    /**
     * <p>
     * A List of resource providers that search for faces-config documents.
     * By default, this contains a provider for the Mojarra, and two other
     * providers to satisfy the requirements of the specification.
     * </p>
     */
    private final List<ConfigurationResourceProvider> facesConfigProviders = unmodifiableList(asList(
            new MetaInfFacesConfigResourceProvider(),
            new WebAppFlowConfigResourceProvider(),
            new WebFacesConfigResourceProvider()
    ));

    /**
     * <p>
     * A List of resource providers that search for faces-config documents.
     * By default, this contains a provider for the Mojarra, and one other
     * providers to satisfy the requirements of the specification.
     * </p>
     */
    private final List<ConfigurationResourceProvider> facesletsTagLibConfigProviders = unmodifiableList(asList(
            new MetaInfFaceletTaglibraryConfigProvider(),
            new WebFaceletTaglibResourceProvider()
    ));
    
    /**
     * <p>
     *  The chain of {@link ConfigProcessor} instances to processing of
     *  facelet-taglib documents.
     * </p>
     */
    private final ConfigProcessor faceletTaglibConfigProcessor = new FaceletTaglibConfigProcessor();

    

    // ---------------------------------------------------------- Public STATIC Methods

    public static ConfigManager createInstance(ServletContext servletContext) {
        ConfigManager result = new ConfigManager();
        servletContext.setAttribute(CONFIG_MANAGER_INSTANCE_KEY, result);
        return result;
    }
    
    /**
     * @return a <code>ConfigManager</code> instance
     */
    public static ConfigManager getInstance(ServletContext servletContext) {
        return (ConfigManager) servletContext.getAttribute(CONFIG_MANAGER_INSTANCE_KEY);
    }
    
    /**
     * @return the results of the annotation scan task
     */
    public static Map<Class<? extends Annotation>, Set<Class<?>>> getAnnotatedClasses(FacesContext ctx) {

        Map<String, Object> appMap = ctx.getExternalContext().getApplicationMap();

        @SuppressWarnings("unchecked")
        Future<Map<Class<? extends Annotation>, Set<Class<?>>>> scanTask = (Future<Map<Class<? extends Annotation>, Set<Class<?>>>>) 
            appMap.get(ANNOTATIONS_SCAN_TASK_KEY);
        
        try {
            return scanTask != null ? scanTask.get() : emptyMap();
        } catch (InterruptedException | ExecutionException e) {
            throw new FacesException(e);
        }

    }
    
    public static void removeInstance(ServletContext servletContext) {
        servletContext.removeAttribute(CONFIG_MANAGER_INSTANCE_KEY);
    }
    
    
    
    // ---------------------------------------------------------- Public instance Methods
    

    /**
     * <p>
     *   This method bootstraps JSF based on the parsed configuration resources.
     * </p>
     *
     * @param servletContext the <code>ServletContext</code> for the application that
     *  requires initialization
     */
    public void initialize(ServletContext servletContext, InitFacesContext facesContext) {

        if (!hasBeenInitialized(servletContext)) {
        	
        		WebConfiguration webConfig = WebConfiguration.getInstance(servletContext);
            boolean validating = webConfig.isOptionEnabled(ValidateFacesConfigFiles);
            boolean useParallelInit = webConfig.isOptionEnabled(EnableParallelInit);
            boolean useThreads = webConfig.isOptionEnabled(EnableThreading);
            
            initializedContexts.add(servletContext);
            initializeConfigProcessers(servletContext, facesContext, useParallelInit);
            ExecutorService executor = null;
            
            try {
                
                if (useThreads) {
                    executor = createExecutorService();
                }
                
                // Obtain and merge the XML and Programmatic documents
                DocumentInfo[] facesDocuments = mergeDocuments(
                        getXMLDocuments(servletContext, getFacesConfigResourceProviders(), executor, validating), 
                        getProgrammaticDocuments(getConfigPopulators()));
                
                FacesConfigInfo lastFacesConfigInfo = new FacesConfigInfo(facesDocuments[facesDocuments.length - 1]);

                facesDocuments = sortDocuments(facesDocuments, lastFacesConfigInfo);

                InjectionProvider containerConnector = InjectionProviderFactory.createInstance(facesContext.getExternalContext());
                facesContext.getAttributes().put(INJECTION_PROVIDER_KEY, containerConnector);

                boolean isFaceletsDisabled = isFaceletsDisabled(webConfig, lastFacesConfigInfo);
                
                if (!lastFacesConfigInfo.isWebInfFacesConfig() || !lastFacesConfigInfo.isMetadataComplete()) {
                    findAnnotations(facesDocuments, containerConnector, servletContext, facesContext, executor);
                }

                // See if the app is running in a HA enabled env               
                if (containerConnector instanceof HighAvailabilityEnabler) {                   
                    ((HighAvailabilityEnabler)containerConnector).enableHighAvailability(servletContext);
                }
                
                // Process the ordered and merged documents
                // This invokes a chain or processors where each processor grabs its own elements of interest
                // from each document.
                
                DocumentInfo[] facesDocuments2 = facesDocuments;
                configProcessors.subList(0, 3).stream().forEachOrdered(e -> {
                    try {
                        e.process(servletContext, facesContext, facesDocuments2);
                    } catch (Exception e2) {
                        // TODO Auto-generated catch block
                        e2.printStackTrace();
                    }
                });
                
                long parentThreadId = Thread.currentThread().getId();
                ClassLoader parentContextClassLoader = Thread.currentThread().getContextClassLoader();
                
                ThreadContext threadContext = getThreadContext(containerConnector);
                Object parentWebContext = threadContext != null ? threadContext.getParentWebContext() : null;
                
                Application application = facesContext.getApplication();
                if (application == null) {
                		LOGGER.severe("Application is null!");
                }
                            
                stream(configProcessors.subList(3, configProcessors.size()).spliterator(), useParallelInit).forEach(e -> {
                    
                    long currentThreadId = Thread.currentThread().getId();
                    
                    LOGGER.info(
                    		"Executing config processer: " + e.getClass() + 
                    		" parallel: " + useParallelInit + 
                    		" thread id: " + currentThreadId
                    	);
                    
                    InitFacesContext initFacesContext = null;
                    if (currentThreadId != parentThreadId) {
                        Thread.currentThread().setContextClassLoader(parentContextClassLoader);
                        initFacesContext = InitFacesContext.getInstance(servletContext);
                        if (parentWebContext != null) {
                            threadContext.propagateWebContextToChild(parentWebContext);
                        }
                    
                    } else {
                        initFacesContext = facesContext;
                    }
                    
                    try {
                        e.process(servletContext, initFacesContext, facesDocuments2);
                    } catch (Exception e1) {
                        // TODO Auto-generated catch block
                        e1.printStackTrace();
                    } finally {
                        if (currentThreadId != parentThreadId) {
                            Thread.currentThread().setContextClassLoader(null);
                            if (parentWebContext != null) {
                                threadContext.clearChildContext();
                            }
                        }
                        
                    }
                });
                
                
                if (!isFaceletsDisabled) {
                    faceletTaglibConfigProcessor.process(
                          servletContext, 
                          facesContext,
                          getXMLDocuments(
                              servletContext,
                              getFaceletConfigResourceProviders(),
                              executor,
                              validating));
                }

            } catch (Exception e) {
                // Clear out any configured factories
                releaseFactories();
                
                Throwable t = e;
                if (!(e instanceof ConfigurationException)) {
                    t = new ConfigurationException("CONFIGURATION FAILED! " + t.getMessage(), t);
                }
                
                throw (ConfigurationException)t;
            } finally {
                if (executor != null) {
                    executor.shutdown();
                }
                servletContext.removeAttribute(ANNOTATIONS_SCAN_TASK_KEY);
            }
        }

        DbfFactory.removeSchemaMap(servletContext);
    }
    
    /**
     * @param servletContext
     *            the <code>ServletContext</code> for the application in question
     * @return <code>true</code> if this application has already been initialized, otherwise returns </code>fase</code>
     */
    public boolean hasBeenInitialized(ServletContext servletContext) {
        return initializedContexts.contains(servletContext);
    }
    
    
    
    // --------------------------------------------------------- Private Methods
    
    
    
    /**
     * Execute the Task responsible for finding annotation classes
     * 
     */
    private void findAnnotations(DocumentInfo[] facesDocuments, InjectionProvider containerConnector, ServletContext servletContext, InitFacesContext context, ExecutorService executor) {
        
        ProvideMetadataToAnnotationScanTask taskMetadata = new ProvideMetadataToAnnotationScanTask(facesDocuments, containerConnector);
        
        Future<Map<Class<? extends Annotation>, Set<Class<?>>>> annotationScan;
        
        if (executor != null) {
            annotationScan = executor.submit(new FindAnnotatedConfigClasses(servletContext, context, taskMetadata));
        } else {
            annotationScan = new FutureTask<>(new FindAnnotatedConfigClasses(servletContext, context, taskMetadata));
            ((FutureTask<Map<Class<? extends Annotation>, Set<Class<?>>>>) annotationScan).run();
        }
        
        pushTaskToContext(servletContext, annotationScan);
    }
    
    /**
     * Push the provided <code>Future</code> to the specified <code>ServletContext</code>.
     */
    private void pushTaskToContext(ServletContext sc, Future<Map<Class<? extends Annotation>, Set<Class<?>>>> scanTask) {
        sc.setAttribute(ANNOTATIONS_SCAN_TASK_KEY, scanTask);
    }

    private List<ConfigurationResourceProvider> getFacesConfigResourceProviders() {
        return getConfigurationResourceProviders(facesConfigProviders, FacesConfig);
    }

    private List<ConfigurationResourceProvider> getFaceletConfigResourceProviders() {
        return getConfigurationResourceProviders(facesletsTagLibConfigProviders, FaceletConfig);
    }

    private List<ConfigurationResourceProvider> getConfigurationResourceProviders(List<ConfigurationResourceProvider> defaultProviders, ConfigurationResourceProviderFactory.ProviderType providerType) {

        ConfigurationResourceProvider[] customProviders = createProviders(providerType);
        if (customProviders.length == 0) {
            return defaultProviders;
        } 
            
        List<ConfigurationResourceProvider> providers = new ArrayList<>(defaultProviders);
        
        // Insert the custom providers after the META-INF providers and
        // before those that scan /WEB-INF
        providers.addAll((defaultProviders.size() - 1), asList(customProviders));
        
        return unmodifiableList(providers);
    }
    
    private void initializeConfigProcessers(ServletContext servletContext, FacesContext facesContext, boolean useParallelInit) {
    		stream(configProcessors.spliterator(), false).forEach(e -> e.initializeClassMetadataMap(servletContext, facesContext));
    }
    
    private List<ApplicationConfigurationPopulator> getConfigPopulators() {
        
        List<ApplicationConfigurationPopulator> configPopulators = new ArrayList<>();
        
        try {
            configPopulators.add( (ApplicationConfigurationPopulator)
                Class.forName("com.sun.faces.config.configpopulator.JsfRIRuntimePopulator_Generated")
                     .newInstance());
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e1) {
            throw new FacesException(e1);
        }
        
        ServiceLoader.load(ApplicationConfigurationPopulator.class)
                     .forEach(e -> configPopulators.add(e));
        
        return configPopulators;
    }
    
    /**
     * Utility method to check if JSF 2.0 Facelets should be disabled, but that doesn't perform <em>the</em> 
     * check unless <code>lastFacesConfigInfo</code> is indeed *the* WEB-INF/faces-config.xml
     * 
     * @param webConfig configuration for this application
     * @param lastFacesConfigInfo object representing WEB-INF/faces-config.xml
     * @return <code>true</code> if Facelets should be disabled
     */
    private boolean isFaceletsDisabled(WebConfiguration webConfig, FacesConfigInfo lastFacesConfigInfo) {
        if (lastFacesConfigInfo.isWebInfFacesConfig()) {
            return _isFaceletsDisabled(webConfig, lastFacesConfigInfo);
        } 
            
        return 
            webConfig.isOptionEnabled(DisableFaceletJSFViewHandler) || 
            webConfig.isOptionEnabled(DisableFaceletJSFViewHandlerDeprecated);
    }


    /**
     * Utility method to check if JSF 2.0 Facelets should be disabled.
     * 
     * <p>
     * If it's not explicitly disabled by the context init parameter, then
     * check the version of the WEB-INF/faces-config.xml document.  If the version
     * is less than 2.0, then override the default value for the context init
     * parameter so that other parts of the system that use that config option
     * will know it has been disabled.
     * </p>
     *
     * <p>
     * <em>NOTE:</em> Since this method overrides a configuration value, it should
     * be called before *any* document parsing is performed the configuration
     * value may be queried by the <code>ConfigParser</code>s.
     * </p>
     *
     * @param webconfig configuration for this application
     * @param facesConfigInfo object representing WEB-INF/faces-config.xml
     * @return <code>true</code> if Facelets should be disabled
     */
    private boolean _isFaceletsDisabled(WebConfiguration webconfig, FacesConfigInfo facesConfigInfo) {

        boolean isFaceletsDisabled = 
            webconfig.isOptionEnabled(DisableFaceletJSFViewHandler) ||
            webconfig.isOptionEnabled(DisableFaceletJSFViewHandlerDeprecated);
        
        if (!isFaceletsDisabled) {
            // if not explicitly disabled, make a sanity check against
            // /WEB-INF/faces-config.xml
            isFaceletsDisabled = !facesConfigInfo.isVersionGreaterOrEqual(2.0);
            webconfig.overrideContextInitParameter(DisableFaceletJSFViewHandler, isFaceletsDisabled);
        }
        
        return isFaceletsDisabled;
    }

    /**
     * Publishes a {@link javax.faces.event.PostConstructApplicationEvent} event for the current
     * {@link Application} instance.
     */
    void publishPostConfigEvent() {

        FacesContext ctx = FacesContext.getCurrentInstance();
        Application app = ctx.getApplication();
        
        if (null == ((InitFacesContext) ctx).getELContext()) {
            ELContext elContext = new ELContextImpl(app.getELResolver());
            elContext.putContext(FacesContext.class, ctx);
            ExpressionFactory exFactory = ELUtils.getDefaultExpressionFactory(ctx);
            if (null != exFactory) {
                elContext.putContext(ExpressionFactory.class, exFactory);
            }
            
            UIViewRoot root = ctx.getViewRoot();
            if (null != root) {
                elContext.setLocale(root.getLocale());
            }
            
            ELContextListener[] listeners = app.getELContextListeners();
            if (listeners.length > 0) {
                ELContextEvent event = new ELContextEvent(elContext);
                for (ELContextListener listener : listeners) {
                    listener.contextCreated(event);
                }
            }
            
            ((InitFacesContext) ctx).setELContext(elContext);
        }

        app.publishEvent(ctx, PostConstructApplicationEvent.class, Application.class, app);
    }
   

    /**
     * Create a new <code>ExecutorService</code> with
     * {@link #NUMBER_OF_TASK_THREADS} threads.
     */
    private static ExecutorService createExecutorService() {

        int tc = Runtime.getRuntime().availableProcessors();
        if (tc > NUMBER_OF_TASK_THREADS) {
            tc = NUMBER_OF_TASK_THREADS;
        }
        
        try {
            return (ExecutorService) new InitialContext().lookup("java:comp/env/concurrent/ThreadPool");
        } catch (NamingException e) {
            // Ignore
        }
        
        return Executors.newFixedThreadPool(tc);
    }
    
    private ThreadContext getThreadContext(InjectionProvider containerConnector) {
        if (containerConnector instanceof ThreadContext) {
            return (ThreadContext) containerConnector;
        }
        
        return null;
    }

    /**
     * Calls through to {@link javax.faces.FactoryFinder#releaseFactories()}
     * ignoring any exceptions.
     */
    private void releaseFactories() {
        try {
            FactoryFinder.releaseFactories();
        } catch (FacesException ignored) {
            LOGGER.log(FINE, "Exception thrown from FactoryFinder.releaseFactories()", ignored);
        }
    }
    
    /**
     * This method will remove any information about the application.
     * 
     * @param facesContext
     *            the <code>FacesContext</code> for the application that needs to be removed
     * @param servletContext
     *            the <code>ServletContext</code> for the application that needs to be removed
     */
    public void destroy(ServletContext servletContext, FacesContext facesContext) {
        boolean useParallelInit = WebConfiguration.getInstance(servletContext).isOptionEnabled(EnableParallelInit);
        
        useParallelInit = false; // tmp
    	
        stream(configProcessors.spliterator(), useParallelInit).forEach(e -> e.destroy(servletContext, facesContext));
        initializedContexts.remove(servletContext);
    }


}
