package org.example.util;

import cn.hutool.core.lang.Console;

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
        double deltaU = 121 - 111;
        double limitU = 115.2 - 111;
        double limitQ = 1;
        double deadZone = 0.15f;
        double rampRate = -10;
        double tempRePowerOrder = 17.64;
        tempRePowerOrder += DeltaQInDroopCurve(deltaU, deadZone, rampRate, limitU, limitQ);  // 1Mvar/2s时 0.2Mvar/s
        Console.log("tempRePowerOrder:{}", tempRePowerOrder);
    }

     static double DeltaQInDroopCurve(double deltaU, double deadZone, double rampRate, double limitU, double limitQ)
    {
        Console.log(String.format("deltaU:%f, deadZone:%f, rampRate:%f, limitU:%f, limitQ:%f\n", deltaU, deadZone, rampRate, limitU, limitQ));
        // 死区内
        if (Math.abs(deltaU) <= deadZone) {
            return 0;
        }
        // 边界外
        if (Math.abs(deltaU) >= limitU) {
            return (deltaU * rampRate > 0 ? 1 : -1) * limitQ;
        }
        // 边界内
        Console.log("deltaU - (deltaU > 0 ? 1 : -1) * deadZone = {}", deltaU - (deltaU > 0 ? 1 : -1) * deadZone);
        double deltaQ = rampRate * (deltaU - (deltaU > 0 ? 1 : -1) * deadZone);

        Console.log("deltaQ = ", deltaQ);
        if (Math.abs(deltaU) >= limitU || Math.abs(deltaQ) >= limitQ) {
            return (deltaU * rampRate > 0 ? 1 : -1) * limitQ;
        }
        return deltaQ;
    }


}
