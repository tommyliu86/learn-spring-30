package com.lwf.springIOC;

import org.springframework.util.StringUtils;

/**
 * Created with IntelliJ IDEA.
 *
 * @author: liuwenfei14
 * @date: 2020-05-12 10:30
 */
public class Program {
    public static void main(String[] args) {

       String s="a;b,c.d";
        String[] strings = StringUtils.tokenizeToStringArray(s, ":.,;");
        for (String string : strings) {

            System.out.println("split is :"+ string);
        }

    }
}
