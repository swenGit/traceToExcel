package org.example.util;

/**
 * @description: 放一些service
 * @author: swen
 * @create: 2024/5/20 13:32
 */
public class MyUtil {
    public static String getTime(String input){
        // 提取日期的起始和结束位置
        int startIndex = input.indexOf("[") + 1; // 起始位置为[后的一个字符
        int endIndex = input.indexOf("]"); // 结束位置为]

        // 使用substring方法提取日期
        String date = input.substring(startIndex, endIndex);
        return date;
    }

    public static String getTimeAGVC(String input){
        // 提取日期的起始和结束位置
        int startIndex = 0; // 11 / 0
        int endIndex = 23; //

        // 使用substring方法提取日期
        String date = input.substring(startIndex, endIndex);
        return date;
    }

    public static String getDate(String input) {
        // 提取日期的起始和结束位置
        int startIndex = input.indexOf("sanywind.trace") + 15;
        int endIndex = startIndex + 10; //

        // 使用substring方法提取日期
        String date = input.substring(startIndex, endIndex);
        return date;
    }

    public static void main(String[] args) {
        System.out.println(Integer.MAX_VALUE + 1);
    }


}
