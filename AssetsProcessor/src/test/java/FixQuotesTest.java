/*
 * Click nbfs://nbhost/SystemFileSystem/Templates/Licenses/license-default.txt to change this license
 * Click nbfs://nbhost/SystemFileSystem/Templates/UnitTests/JUnit5TestClass.java to edit this template
 */

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/**
 *
 * @author homberghp {@code <pieter.van.den.hombergh@gmail.com>}
 */
public class FixQuotesTest {


    @Test
    public void test1() {
        String in = "\"Hello \"old\" World\"";
        System.out.println("in = " + in);
        String s =AssetsProcessor.fixQuotes(in);
        System.out.println("s = " + s);

        assertEquals("\"Hello 'old' World\"", s);

    }
    @Test
    public void test2() {
        String in = "'Hello 'old' World'";
        System.out.println("in = " + in);
        String s =AssetsProcessor.fixQuotes(in);
        System.out.println("s = " + s);

        assertEquals("'Hello \"old\" World'", s);

    }
}
