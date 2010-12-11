/*
 * Copyright 2009-2010 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.mbte.groovypp.runtime;

import groovy.lang.GString;
import groovy.lang.GroovyRuntimeException;
import org.codehaus.groovy.runtime.DefaultGroovyMethodsSupport;
import org.codehaus.groovy.runtime.DefaultGroovyMethods;
import org.codehaus.groovy.runtime.InvokerHelper;
import org.codehaus.groovy.runtime.RegexSupport;
import org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation;
import org.codehaus.groovy.runtime.typehandling.NumberMath;
import org.codehaus.groovy.transform.powerassert.ValueRecorder;
import org.mbte.groovypp.compiler.TypeUtil;

import java.io.Writer;
import java.lang.reflect.Array;
import java.math.BigDecimal;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.codehaus.groovy.runtime.typehandling.DefaultTypeTransformation.*;

public class DefaultGroovyPPMethods extends DefaultGroovyMethodsSupport {
    public static Number plus(Number self, Number other) {
        return NumberMath.add(self, other);
    }

    public static Number minus(Number self, Number other) {
        return NumberMath.subtract(self, other);
    }

    public static Number multiply(Number self, Number other) {
        return NumberMath.multiply(self, other);
    }

    public static Number div(Number self, Number other) {
        return NumberMath.divide(self, other);
    }

    public static int intdiv(byte left, byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(byte left, short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(byte left, int right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(byte left, long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(byte left, Byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(byte left, Short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(byte left, Integer right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(byte left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(short left, byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(short left, short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(short left, int right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(short left, long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(short left, Byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(short left, Short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(short left, Integer right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(short left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(int left, byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(int left, short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(int left, int right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(int left, long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(int left, Byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(int left, Short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(int left, Integer right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(int left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, byte right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, short right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, int right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, long right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, Byte right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, Short right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, Integer right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(long left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(Byte left, byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Byte left, short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Byte left, int right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(Byte left, long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(Byte left, Byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Byte left, Short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Byte left, Integer right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(Byte left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(Short left, byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Short left, short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Short left, int right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(Short left, long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(Short left, Byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Short left, Short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Short left, Integer right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(Short left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(Integer left, byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Integer left, short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Integer left, int right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(Integer left, long right) {
        return ((long)left) / ((long)right);
    }


    public static int intdiv(Integer left, Byte right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Integer left, Short right) {
        return ((int)left) / ((int)right);
    }


    public static int intdiv(Integer left, Integer right) {
        return ((int)left) / ((int)right);
    }


    public static long intdiv(Integer left, Long right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, byte right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, short right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, int right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, long right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, Byte right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, Short right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, Integer right) {
        return ((long)left) / ((long)right);
    }


    public static long intdiv(Long left, Long right) {
        return ((long)left) / ((long)right);
    }

    public static Boolean box(boolean value) {
        return value ? Boolean.TRUE : Boolean.FALSE;
    }

    public static Byte box(byte value) {
        return value;
    }

    public static Character box(char value) {
        return value;
    }

    public static Short box(short value) {
        return value;
    }

    public static Integer box(int value) {
        return value;
    }

    public static Long box(long value) {
        return value;
    }

    public static Float box(float value) {
        return value;
    }

    public static Double box(double value) {
        return value;
    }

    public static Class getClass(byte value) {
        return Byte.TYPE;
    }

    public static Class getClass(short value) {
        return Short.TYPE;
    }

    public static Class getClass(int value) {
        return Integer.TYPE;
    }

    public static Class getClass(char value) {
        return Character.TYPE;
    }

    public static Class getClass(long value) {
        return Long.TYPE;
    }

    public static Class getClass(float value) {
        return Float.TYPE;
    }

    public static Class getClass(double value) {
        return Double.TYPE;
    }

    public static Class getClass(boolean value) {
        return Boolean.TYPE;
    }

    public static String[] gstringArrayToStringArray(GString[] data) {
        if (data == null)
            return null;

        String[] strings = new String[data.length];
        for (int i = 0; i < strings.length; i++) {
            strings[i] = data[i].toString();
        }

        return strings;
    }

    public static Pattern bitwiseNegate(GString self) {
        return DefaultGroovyMethods.bitwiseNegate(self.toString());
    }

    public static Iterator<String> iterator(String self) {
        return DefaultGroovyMethods.toList(self).iterator();
    }

    public static void print(Writer self, Object value) {
        DefaultGroovyMethods.print(self, value);
    }

    public static void println(Writer self, Object value) {
        DefaultGroovyMethods.println(self, value);
    }

    public static boolean equals(Object left, Object right) {
        return left == right || !(left == null || right == null) && left.equals(right);
    }

    public static boolean equals(List left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;
        Class rightClass = right.getClass();
        if (rightClass.isArray()) {
            return compareArrayEqual(left, right);
        }
        if (right instanceof List)
            return DefaultGroovyMethods.equals(left, (List)right);
        return false;
    }

    public static boolean compareEqual(Object left, Object right) {
        if (left == right) return true;
        if (left == null || right == null) return false;

        if (left instanceof Comparable) {
            return compareToWithEqualityCheck(left, right) == 0;
        }

        // handle arrays on both sides as special case for efficiency
        Class leftClass = left.getClass();
        Class rightClass = right.getClass();
        if (leftClass.isArray() && rightClass.isArray()) {
            return compareArrayEqual(left, right);
        }
        if (leftClass.isArray() && leftClass.getComponentType().isPrimitive()) {
            left = primitiveArrayToList(left);
        }
        if (rightClass.isArray() && rightClass.getComponentType().isPrimitive()) {
            right = primitiveArrayToList(right);
        }
        if (left instanceof Object[] && right instanceof List) {
            return DefaultGroovyMethods.equals((Object[]) left, (List) right);
        }
        if (left instanceof List && right instanceof Object[]) {
            return DefaultGroovyMethods.equals((List) left, (Object[]) right);
        }
        if (left instanceof List && right instanceof List) {
            return DefaultGroovyMethods.equals((List) left, (List) right);
        }
        return left.equals(right);
    }

    private static boolean isValidCharacterString(Object value) {
        if (value instanceof String) {
            String s = (String) value;
            if (s.length() == 1) {
                return true;
            }
        }
        return false;
    }

    public static int compareToWithEqualityCheck(Object left, Object right) {
        if (left == right) {
            return 0;
        }
        if (left == null) {
            return -1;
        }
        else if (right == null) {
            return 1;
        }
        if (left instanceof Comparable) {
            if (left instanceof Number) {
                if (isValidCharacterString(right)) {
                    return DefaultGroovyMethods.compareTo((Number) left, (Character) box(castToChar(right)));
                }
                if (right instanceof Character || right instanceof Number) {
                    return DefaultGroovyMethods.compareTo((Number) left, castToNumber(right));
                }
            }
            else if (left instanceof Character) {
                if (isValidCharacterString(right)) {
                    return DefaultGroovyMethods.compareTo((Character)left,(Character)box(castToChar(right)));
                }
                if (right instanceof Number) {
                    return DefaultGroovyMethods.compareTo((Character)left,(Number)right);
                }
            }
            else if (right instanceof Number) {
                if (isValidCharacterString(left)) {
                    return DefaultGroovyMethods.compareTo((Character)box(castToChar(left)),(Number) right);
                }
            }
            else if (left instanceof String && right instanceof Character) {
                return ((String) left).compareTo(right.toString());
            }
            else if (left instanceof String && right instanceof GString) {
                return ((String) left).compareTo(right.toString());
            }
            if (left.getClass().isAssignableFrom(right.getClass())
                    || (right.getClass() != Object.class && right.getClass().isAssignableFrom(left.getClass())) //GROOVY-4046
                    || (left instanceof GString && right instanceof String)) {
                Comparable comparable = (Comparable) left;
                return comparable.compareTo(right);
            }
        }

        return -1; // anything other than 0
    }

    public static BigDecimal asBigDecimal(Number self) {
        return self instanceof BigDecimal ? (BigDecimal) self : self instanceof Float || self instanceof Double ? new BigDecimal(self.doubleValue()) : new BigDecimal(self.toString());
    }

    public static boolean asBooleanDynamic(Object object) {
        if(object == null)
            return false;

        if(object instanceof Boolean)
            return (Boolean) object;

        if(object instanceof Number) {
            return ((Number)object).doubleValue() != 0;
        }

        if(object instanceof Collection) {
            return !((Collection)object).isEmpty();
        }

        if(object instanceof Character) {
            return ((Character) object) != 0;
        }

        if(object instanceof Map)
            return !((Map)object).isEmpty();

        if(object instanceof Iterator)
            return ((Iterator)object).hasNext();

        if(object instanceof Enumeration)
            return ((Enumeration)object).hasMoreElements();

        if(object instanceof CharSequence)
            return ((CharSequence)object).length() > 0;

        if(object.getClass().isArray()) {
            return Array.getLength(object) > 0;
        }

        if(object instanceof Matcher) {
            final Matcher matcher = (Matcher) object;
            RegexSupport.setLastMatcher(matcher);
            return matcher.find();
        }

        return true;
    }

    public static <T> T gppRecord(ValueRecorder recorder, T value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static byte gppRecord(ValueRecorder recorder, byte value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static short gppRecord(ValueRecorder recorder, short value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static boolean gppRecord(ValueRecorder recorder, boolean value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static char gppRecord(ValueRecorder recorder, char value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static float gppRecord(ValueRecorder recorder, float value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static double gppRecord(ValueRecorder recorder, double value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static long gppRecord(ValueRecorder recorder, long value, int column) {
        recorder.record(value, column);
        return value;
    }

    public static int gppRecord(ValueRecorder recorder, int value, int column) {
        recorder.record(value, column);
        return value;
    }
}
