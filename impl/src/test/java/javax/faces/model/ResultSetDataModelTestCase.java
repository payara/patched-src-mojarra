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

package javax.faces.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.sun.faces.mock.MockResultSet;
import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static junit.framework.Assert.assertNull;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;
import junit.framework.Test;
import junit.framework.TestSuite;

/**
 * <p>
 * Unit tests for {@link ArrayDataModel}.</p>
 */
public class ResultSetDataModelTestCase extends DataModelTestCaseBase {

    // ------------------------------------------------------------ Constructors
    /**
     * Construct a new instance of this test case.
     *
     * @param name Name of the test case
     */
    public ResultSetDataModelTestCase(String name) {
        super(name);
    }

    // ------------------------------------------------------ Instance Variables
    // The ResultSet passed to our ResultSetDataModel
    protected MockResultSet result = null;

    // ---------------------------------------------------- Overall Test Methods
    // Set up instance variables required by this test case.
    @Override
    public void setUp() throws Exception {
        beans = new BeanTestImpl[5];
        for (int i = 0; i < beans.length; i++) {
            beans[i] = new BeanTestImpl();
        }
        configure();
        result = new MockResultSet(beans);
        model = new ResultSetDataModel(result);
        super.setUp();
    }

    // Return the tests included in this test case.
    public static Test suite() {
        return (new TestSuite(ResultSetDataModelTestCase.class));
    }

    // ------------------------------------------------- Individual Test Methods
    // Test ((Map) getRowData()).containsKey()
    public void testRowDataContainsKey() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;

        // Test exact match on column names
        assertTrue(map.containsKey("booleanProperty"));
        assertTrue(map.containsKey("booleanSecond"));
        assertTrue(map.containsKey("byteProperty"));
        assertTrue(map.containsKey("doubleProperty"));
        assertTrue(map.containsKey("floatProperty"));
        assertTrue(map.containsKey("intProperty"));
        assertTrue(map.containsKey("longProperty"));
        assertTrue(map.containsKey("stringProperty"));

        // Test inexact match on column names
        assertTrue(map.containsKey("booleanPROPERTY"));
        assertTrue(map.containsKey("booleanSECOND"));
        assertTrue(map.containsKey("bytePROPERTY"));
        assertTrue(map.containsKey("doublePROPERTY"));
        assertTrue(map.containsKey("floatPROPERTY"));
        assertTrue(map.containsKey("intPROPERTY"));
        assertTrue(map.containsKey("longPROPERTY"));
        assertTrue(map.containsKey("stringPROPERTY"));

        // Test false return on invalid column names
        assertTrue(!map.containsKey("foo"));
        assertTrue(!map.containsKey("FOO"));
        assertTrue(!map.containsKey("bar"));
        assertTrue(!map.containsKey("BAR"));
    }

    // Test ((Map) getRowData()).containsValue()
    public void testRowDataContainsValue() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;

        // Test positive results
        assertTrue(map.containsValue(Boolean.TRUE));
        assertTrue(map.containsValue(Boolean.FALSE));
        assertTrue(map.containsValue(new Byte((byte) 1)));
        assertTrue(map.containsValue(new Double((double) 100.0)));
        assertTrue(map.containsValue(new Float((float) 10.0)));
        assertTrue(map.containsValue(new Integer((int) 1000)));
        assertTrue(map.containsValue(new Long((long) 10000)));
        assertTrue(map.containsValue("This is string 1"));

        // Test negative results
        assertTrue(!map.containsValue("foo"));
        assertTrue(!map.containsValue(new Integer(654321)));
    }

    // Test ((Map) getRowData()).entrySet()
    public void testRowDataEntrySet() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;
        Set set = map.entrySet();

        // Test exact match postive results
        assertTrue(set.contains(new TestEntry("booleanProperty",
                Boolean.FALSE)));
        assertTrue(set.contains(new TestEntry("booleanSecond",
                Boolean.TRUE)));
        assertTrue(set.contains(new TestEntry("byteProperty",
                new Byte((byte) 1))));
        assertTrue(set.contains(new TestEntry("doubleProperty",
                new Double((double) 100.0))));
        assertTrue(set.contains(new TestEntry("floatProperty",
                new Float((float) 10.0))));
        assertTrue(set.contains(new TestEntry("intProperty",
                new Integer((int) 1000))));
        assertTrue(set.contains(new TestEntry("longProperty",
                new Long((long) 10000))));
        assertTrue(set.contains(new TestEntry("stringProperty", "This is string 1")));

        // Test exact match postive results
        assertTrue(set.contains(new TestEntry("booleanPROPERTY",
                Boolean.FALSE)));
        assertTrue(set.contains(new TestEntry("booleanSECOND",
                Boolean.TRUE)));
        assertTrue(set.contains(new TestEntry("bytePROPERTY",
                new Byte((byte) 1))));
        assertTrue(set.contains(new TestEntry("doublePROPERTY",
                new Double((double) 100.0))));
        assertTrue(set.contains(new TestEntry("floatPROPERTY",
                new Float((float) 10.0))));
        assertTrue(set.contains(new TestEntry("intPROPERTY",
                new Integer((int) 1000))));
        assertTrue(set.contains(new TestEntry("longPROPERTY",
                new Long((long) 10000))));
        assertTrue(set.contains(new TestEntry("stringPROPERTY", "This is string 1")));

        // Test negative results
        assertTrue(!set.contains(new TestEntry("foo", "bar")));
        assertTrue(!set.contains(new TestEntry("FOO", "bar")));
        assertTrue(!set.contains(new TestEntry("baz", "bop")));
        assertTrue(!set.contains(new TestEntry("BAZ", "bop")));

        // Test other methods
        assertTrue(!set.isEmpty());

        // Test updating through the entry set
        Iterator entries = set.iterator();
        while (entries.hasNext()) {
            Map.Entry entry = (Map.Entry) entries.next();
            if ("stringProperty".equalsIgnoreCase((String) entry.getKey())) {
                entry.setValue("This is string 1 modified");
            }
        }
        assertEquals("This is string 1 modified",
                beans[1].getStringProperty());
        assertEquals("This is string 1 modified",
                (String) map.get("stringProperty"));
        assertEquals("This is string 1 modified",
                (String) map.get("stringPROPERTY"));
        result.absolute(2); // ResultSet indexing is one-relative
        assertEquals("This is string 1 modified",
                (String) result.getObject("stringProperty"));
    }

    // Test ((Map) getRowData()).get()
    public void testRowDataGet() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;

        // Test exact match on column names
        assertEquals(Boolean.FALSE,
                (Boolean) map.get("booleanProperty"));
        assertEquals(Boolean.TRUE,
                (Boolean) map.get("booleanSecond"));
        assertEquals(new Byte((byte) 1),
                (Byte) map.get("byteProperty"));
        assertEquals(new Double((double) 100.0),
                (Double) map.get("doubleProperty"));
        assertEquals(new Float((float) 10.0),
                (Float) map.get("floatProperty"));
        assertEquals(new Integer((int) 1000),
                (Integer) map.get("intProperty"));
        assertEquals(new Long((long) 10000),
                (Long) map.get("longProperty"));
        assertEquals("This is string 1",
                (String) map.get("stringProperty"));

        // Test inexact match on column names
        assertEquals(Boolean.FALSE,
                (Boolean) map.get("booleanPROPERTY"));
        assertEquals(Boolean.TRUE,
                (Boolean) map.get("booleanSECOND"));
        assertEquals(new Byte((byte) 1),
                (Byte) map.get("bytePROPERTY"));
        assertEquals(new Double((double) 100.0),
                (Double) map.get("doublePROPERTY"));
        assertEquals(new Float((float) 10.0),
                (Float) map.get("floatPROPERTY"));
        assertEquals(new Integer((int) 1000),
                (Integer) map.get("intPROPERTY"));
        assertEquals(new Long((long) 10000),
                (Long) map.get("longPROPERTY"));
        assertEquals("This is string 1",
                (String) map.get("stringPROPERTY"));

        // Test null return on non-existent column names
        assertNull(map.get("foo"));
        assertNull(map.get("FOO"));
        assertNull(map.get("bar"));
        assertNull(map.get("bar"));
    }

    // Test ((Map) getRowData()).keySet()
    public void testRowDataKeySet() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;
        Set set = map.keySet();

        // Test exact match postive results
        assertTrue(set.contains("booleanProperty"));
        assertTrue(set.contains("booleanSecond"));
        assertTrue(set.contains("byteProperty"));
        assertTrue(set.contains("doubleProperty"));
        assertTrue(set.contains("floatProperty"));
        assertTrue(set.contains("intProperty"));
        assertTrue(set.contains("longProperty"));
        assertTrue(set.contains("stringProperty"));

        // Test inexact match positive results
        assertTrue(set.contains("booleanPROPERTY"));
        assertTrue(set.contains("booleanSECOND"));
        assertTrue(set.contains("bytePROPERTY"));
        assertTrue(set.contains("doublePROPERTY"));
        assertTrue(set.contains("floatPROPERTY"));
        assertTrue(set.contains("intPROPERTY"));
        assertTrue(set.contains("longPROPERTY"));
        assertTrue(set.contains("stringPROPERTY"));

        // Test negative results
        assertTrue(!set.contains("foo"));
        assertTrue(!set.contains("FOO"));
        assertTrue(!set.contains("bar"));
        assertTrue(!set.contains("BAR"));

        // Test other methods
        assertTrue(!set.isEmpty());
    }

    // Test ((Map) getRowData()).put()
    public void testRowDataPut() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;
    }

    // Test unsupported operations on ((Map) getRowData())
    public void testRowDataUnsupported() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;

        // clear()
        try {
            map.clear();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }

        // entrySet()
        Set entrySet = map.entrySet();
        try {
            entrySet.add(new TestEntry("foo", "bar"));
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        List mapEntries = new ArrayList();
        mapEntries.add(new TestEntry("foo", "bar"));
        mapEntries.add(new TestEntry("baz", "bop"));
        try {
            entrySet.addAll(mapEntries);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            entrySet.clear();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            Iterator iterator = entrySet.iterator();
            iterator.next();
            iterator.remove();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            entrySet.remove(new TestEntry("foo", "bar"));
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            entrySet.removeAll(mapEntries);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            entrySet.retainAll(mapEntries);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }

        // keySet()
        Set keySet = map.keySet();
        try {
            keySet.add("foo");
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        List mapKeys = new ArrayList();
        mapKeys.add("foo");
        mapKeys.add("bar");
        try {
            keySet.addAll(mapKeys);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            keySet.clear();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            Iterator iterator = keySet.iterator();
            iterator.next();
            iterator.remove();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            keySet.remove(new TestEntry("foo", "bar"));
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            keySet.removeAll(mapKeys);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            keySet.retainAll(mapKeys);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }

        // remove()
        try {
            map.remove("foo");
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }

        // values()
        Collection values = map.values();
        try {
            values.add("foo");
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        List list = new ArrayList();
        list.add("foo");
        list.add("bar");
        try {
            values.addAll(list);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            values.clear();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            Iterator iterator = values.iterator();
            iterator.next();
            iterator.remove();
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            values.remove("foo");
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            values.removeAll(list);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }
        try {
            values.retainAll(list);
            fail("Should have thrown UnsupportedOperationException");
        } catch (UnsupportedOperationException e) {
            // Expected result
        }

    }

    // Test ((Map) getRowData()).values()
    public void testRowDataValues() throws Exception {
        // Position to row 1 and retrieve the corresponding Map
        model.setRowIndex(1);
        assertTrue(model.isRowAvailable());
        Object data = model.getRowData();
        assertNotNull(data);
        assertTrue(data instanceof Map);
        Map map = (Map) data;
        Collection values = map.values();

        // Test positive results
        assertTrue(values.contains(Boolean.TRUE));
        assertTrue(values.contains(Boolean.FALSE));
        assertTrue(values.contains(new Byte((byte) 1)));
        assertTrue(values.contains(new Double((double) 100.0)));
        assertTrue(values.contains(new Float((float) 10.0)));
        assertTrue(values.contains(new Integer((int) 1000)));
        assertTrue(values.contains(new Long((long) 10000)));
        assertTrue(values.contains("This is string 1"));

        // Test negative results
        assertTrue(!values.contains("foo"));
        assertTrue(!values.contains(new Integer(654321)));

        // Test other methods
        assertTrue(!values.isEmpty());
    }

    // ------------------------------------------------------- Protected Methods
    @Override
    protected BeanTestImpl data() throws Exception {
        Object data = model.getRowData();
        assertTrue(data instanceof Map);
        BeanTestImpl bean = new BeanTestImpl();
        Map map = (Map) data;

        bean.setBooleanProperty(((Boolean) map.get("booleanProperty")).booleanValue());
        bean.setBooleanSecond(((Boolean) map.get("booleanSecond")).booleanValue());
        bean.setByteProperty(((Byte) map.get("byteProperty")).byteValue());
        bean.setDoubleProperty(((Double) map.get("doubleProperty")).doubleValue());
        bean.setFloatProperty(((Float) map.get("floatProperty")).floatValue());
        bean.setIntProperty(((Integer) map.get("intProperty")).intValue());
        bean.setLongProperty(((Long) map.get("longProperty")).longValue());
        bean.setNullProperty((String) map.get("nullProperty"));
        bean.setShortProperty(((Short) map.get("shortProperty")).shortValue());
        bean.setStringProperty((String) map.get("stringProperty"));
        bean.setWriteOnlyProperty((String) map.get("writeOnlyPropertyValue"));

        return (bean);
    }

    class TestEntry implements Map.Entry {

        public TestEntry(Object key, Object value) {
            this.key = key;
            this.value = value;
        }

        private Object key;
        private Object value;

        @Override
        public Object getKey() {
            return key;
        }

        @Override
        public Object getValue() {
            return value;
        }

        @Override
        public Object setValue(Object value) {
            Object previous = this.value;
            this.value = value;
            return previous;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Map.Entry)) {
                return (false);
            }
            Map.Entry e = (Map.Entry) o;
            return (key == null
                    ? e.getKey() == null : key.equals(e.getKey()))
                    && (value == null
                    ? e.getValue() == null : value.equals(e.getValue()));
        }
    }
}
