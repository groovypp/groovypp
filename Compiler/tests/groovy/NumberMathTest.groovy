/*
 * Copyright 2009-2011 MBTE Sweden AB.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */





package groovy

import org.codehaus.groovy.runtime.typehandling.NumberMath

/**
 * Basic NumberMath test.
 * @see org.codehaus.groovy.runtime.typehandling.NumberMath
 */
class NumberMathTest extends GroovyShellTestCase {

  void testPromotions() {
    shell.evaluate """
          @Typed
          def u() {
            def C = '1'.toCharacter()
            def B = new Byte("1")
            def I = new Integer(1)
            def L = new Long(1)
            def F = new Float("1.0")
            def D = new Double("1.0")
            def BI = new BigInteger("1")
            def BD = new BigDecimal("1.0")

            //+, -, and * all promote the same way, so sample the matrix
            assert C + B instanceof Integer
            assert C - BD instanceof BigDecimal
            assert B + C instanceof Integer
            assert B + I instanceof Integer
            assert B + F instanceof Double

            assert I + I instanceof Integer
            assert I - F instanceof Double
            assert I * D instanceof Double
            assert I + BI instanceof BigInteger
            assert I - BD instanceof BigDecimal

            assert F * L instanceof Double
            assert D + L instanceof Double
            assert BI - L instanceof BigInteger
            assert BD * L instanceof BigDecimal

            assert F + F instanceof Double
            assert F - BI instanceof Double
            assert F * BD instanceof Double

            assert F + D instanceof Double
            assert BI - D instanceof Double
            assert BD * D instanceof Double

            assert BI + BI instanceof BigInteger
            assert BD - BI instanceof BigDecimal
            assert BD * BD instanceof BigDecimal

            //Division (/) promotes differently so change the expected results:
            assert I / I instanceof BigDecimal
            assert I / F instanceof Double
            assert I / D instanceof Double
            assert I / BI instanceof BigDecimal
            assert I / BD instanceof BigDecimal

            assert F / L instanceof Double
            assert D / L instanceof Double
            assert BI / L instanceof BigDecimal
            assert BD / L instanceof BigDecimal

            assert F / F instanceof Double
            assert F / BI instanceof Double
            assert F / BD instanceof Double

            assert F / D instanceof Double
            assert BI / D instanceof Double
            assert BD / D instanceof Double

            assert BI / BI instanceof BigDecimal
            assert BD / BI instanceof BigDecimal
            assert BD / BD instanceof BigDecimal


          }
          u()
        """
  }

  void testOperations() {
    shell.evaluate """

        @Typed
        def u() {
          def I1 = new Integer(1)
          def I2 = new Integer(2)
          def I3 = new Integer(3)
          def L1 = new Long(1)
          def L2 = new Long(2)
          def L3 = new Long(3)
          def F1 = new Float("1.0")
          def F2 = new Float("2.0")
          def D1 = new Double("1.0")
          def D2 = new Double("2.0")
          def BI1 = new BigInteger("1")
          def BI2 = new BigInteger("2")
          def BD1 = new BigDecimal("1.0")
          def BD2 = new BigDecimal("2.0")
          def BD20 = new BigDecimal("2.00")

          assert I1 / I2 == 0.5
          assert I3 / I2 == 1.5
          assert F1 / F2 instanceof Double
          junit.framework.Assert.assertEquals F1 / F2, 0.5, 0.0000000001
          assert D1 / D2 instanceof Double
          junit.framework.Assert.assertEquals D1 / D2, 0.5, 0.0000000001

          assert I1.intdiv(I2) instanceof Integer
          assert I1.intdiv(I2) == 0

          assert I3.intdiv(I2) instanceof Integer
          assert I3.intdiv(I2) == 1

          assert L1.intdiv(I2) instanceof Long
          assert L1.intdiv(I2) == 0

          assert L3.intdiv(L2) instanceof Long
          assert L3.intdiv(L2) == 1

          assert BI1.intdiv(BI2) instanceof BigInteger
          assert BI1.intdiv(BI2) == 0

          assert I1.div(I3) instanceof BigDecimal
          assert I1.div(I3) == new BigDecimal("0.3333333333")

          assert I2.div(I3) instanceof BigDecimal
          assert I2.div(I3) == new BigDecimal("0.6666666667")

          assert I1 / BD2 instanceof BigDecimal

          //Test keeping max scale of (L, R or 10)
          def BBD1 = new BigDecimal("0.12345678901234567")
          assert BD1 + BBD1 == new BigDecimal("1.12345678901234567")

          def BBD2 = new BigDecimal(".000000000000000008")
          assert BBD1 + BBD2 == new BigDecimal("0.123456789012345678")

        }

        u()
      """

  }

  void testUnsupportedIntDivision() {
    shell.evaluate """

          @Typed
          def u() {
            boolean catchVisited = false;
            try {
                1.0.intdiv(3)
            } catch (UnsupportedOperationException uoe) {
                catchVisited = true;
            }
            assert catchVisited;

            catchVisited = false
            try {
                1.0G.intdiv(3)
            } catch (UnsupportedOperationException uoe) {
                catchVisited = true;
            }
            assert catchVisited;
          }

          u()
        """
  }

  void testGetMath() {
    shell.evaluate """

          @Typed
          def u() {
            assert 20 == new Short("10") << 1
            assert 2 == new Byte("1") << 1
          }
          u()
        """
  }

  void testLongDivAssign() {
    shell.evaluate """

          @Typed
          def u() {
            def d = 100L
            d /= 33L
            assert d.class == BigDecimal.class
          }

          u()
        """
  }

  void testIntegerPlusCastException() {
    shell.evaluate """

          @Typed
          def u() {
              def i = 12
              i += " angry men"
              assert ['1','2','n','g'] == i.chars[[0,1,-1,5]];
          }
          u()
        """
  }

  void testIntegerBitwiseNegate() {
    def res = shell.evaluate("""
          @Typed
          def u() {
              ~0
          }
          u()
        """)
    assertEquals(-1, res)
  }

  void testIntegerIntdiv() {
    def res = shell.evaluate("""
          @Typed
          def u() {
byte v_byte = 5

      byte v_byte_byte = 2
      assert v_byte.intdiv(v_byte_byte).class == int


      short v_byte_short = 2
      assert v_byte.intdiv(v_byte_short).class == int


      int v_byte_int = 2
      assert v_byte.intdiv(v_byte_int).class == int


      long v_byte_long = 2
      assert v_byte.intdiv(v_byte_long).class == long


      Byte v_byte_Byte = 2
      assert v_byte.intdiv(v_byte_Byte).class == int


      Short v_byte_Short = 2
      assert v_byte.intdiv(v_byte_Short).class == int


      Integer v_byte_Integer = 2
      assert v_byte.intdiv(v_byte_Integer).class == int


      Long v_byte_Long = 2
      assert v_byte.intdiv(v_byte_Long).class == long

short v_short = 5

      byte v_short_byte = 2
      assert v_short.intdiv(v_short_byte).class == int


      short v_short_short = 2
      assert v_short.intdiv(v_short_short).class == int


      int v_short_int = 2
      assert v_short.intdiv(v_short_int).class == int


      long v_short_long = 2
      assert v_short.intdiv(v_short_long).class == long


      Byte v_short_Byte = 2
      assert v_short.intdiv(v_short_Byte).class == int


      Short v_short_Short = 2
      assert v_short.intdiv(v_short_Short).class == int


      Integer v_short_Integer = 2
      assert v_short.intdiv(v_short_Integer).class == int


      Long v_short_Long = 2
      assert v_short.intdiv(v_short_Long).class == long

int v_int = 5

      byte v_int_byte = 2
      assert v_int.intdiv(v_int_byte).class == int


      short v_int_short = 2
      assert v_int.intdiv(v_int_short).class == int


      int v_int_int = 2
      assert v_int.intdiv(v_int_int).class == int


      long v_int_long = 2
      assert v_int.intdiv(v_int_long).class == long


      Byte v_int_Byte = 2
      assert v_int.intdiv(v_int_Byte).class == int


      Short v_int_Short = 2
      assert v_int.intdiv(v_int_Short).class == int


      Integer v_int_Integer = 2
      assert v_int.intdiv(v_int_Integer).class == int


      Long v_int_Long = 2
      assert v_int.intdiv(v_int_Long).class == long

long v_long = 5

      byte v_long_byte = 2
      assert v_long.intdiv(v_long_byte).class == long


      short v_long_short = 2
      assert v_long.intdiv(v_long_short).class == long


      int v_long_int = 2
      assert v_long.intdiv(v_long_int).class == long


      long v_long_long = 2
      assert v_long.intdiv(v_long_long).class == long


      Byte v_long_Byte = 2
      assert v_long.intdiv(v_long_Byte).class == long


      Short v_long_Short = 2
      assert v_long.intdiv(v_long_Short).class == long


      Integer v_long_Integer = 2
      assert v_long.intdiv(v_long_Integer).class == long


      Long v_long_Long = 2
      assert v_long.intdiv(v_long_Long).class == long

Byte v_Byte = 5

      byte v_Byte_byte = 2
      assert v_Byte.intdiv(v_Byte_byte).class == int


      short v_Byte_short = 2
      assert v_Byte.intdiv(v_Byte_short).class == int


      int v_Byte_int = 2
      assert v_Byte.intdiv(v_Byte_int).class == int


      long v_Byte_long = 2
      assert v_Byte.intdiv(v_Byte_long).class == long


      Byte v_Byte_Byte = 2
      assert v_Byte.intdiv(v_Byte_Byte).class == int


      Short v_Byte_Short = 2
      assert v_Byte.intdiv(v_Byte_Short).class == int


      Integer v_Byte_Integer = 2
      assert v_Byte.intdiv(v_Byte_Integer).class == int


      Long v_Byte_Long = 2
      assert v_Byte.intdiv(v_Byte_Long).class == long

Short v_Short = 5

      byte v_Short_byte = 2
      assert v_Short.intdiv(v_Short_byte).class == int


      short v_Short_short = 2
      assert v_Short.intdiv(v_Short_short).class == int


      int v_Short_int = 2
      assert v_Short.intdiv(v_Short_int).class == int


      long v_Short_long = 2
      assert v_Short.intdiv(v_Short_long).class == long


      Byte v_Short_Byte = 2
      assert v_Short.intdiv(v_Short_Byte).class == int


      Short v_Short_Short = 2
      assert v_Short.intdiv(v_Short_Short).class == int


      Integer v_Short_Integer = 2
      assert v_Short.intdiv(v_Short_Integer).class == int


      Long v_Short_Long = 2
      assert v_Short.intdiv(v_Short_Long).class == long

Integer v_Integer = 5

      byte v_Integer_byte = 2
      assert v_Integer.intdiv(v_Integer_byte).class == int


      short v_Integer_short = 2
      assert v_Integer.intdiv(v_Integer_short).class == int


      int v_Integer_int = 2
      assert v_Integer.intdiv(v_Integer_int).class == int


      long v_Integer_long = 2
      assert v_Integer.intdiv(v_Integer_long).class == long


      Byte v_Integer_Byte = 2
      assert v_Integer.intdiv(v_Integer_Byte).class == int


      Short v_Integer_Short = 2
      assert v_Integer.intdiv(v_Integer_Short).class == int


      Integer v_Integer_Integer = 2
      assert v_Integer.intdiv(v_Integer_Integer).class == int


      Long v_Integer_Long = 2
      assert v_Integer.intdiv(v_Integer_Long).class == long

Long v_Long = 5

      byte v_Long_byte = 2
      assert v_Long.intdiv(v_Long_byte).class == long


      short v_Long_short = 2
      assert v_Long.intdiv(v_Long_short).class == long


      int v_Long_int = 2
      assert v_Long.intdiv(v_Long_int).class == long


      long v_Long_long = 2
      assert v_Long.intdiv(v_Long_long).class == long


      Byte v_Long_Byte = 2
      assert v_Long.intdiv(v_Long_Byte).class == long


      Short v_Long_Short = 2
      assert v_Long.intdiv(v_Long_Short).class == long


      Integer v_Long_Integer = 2
      assert v_Long.intdiv(v_Long_Integer).class == long


      Long v_Long_Long = 2
      assert v_Long.intdiv(v_Long_Long).class == long

          }
          u()
        """)
  }
}
