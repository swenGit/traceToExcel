package org.example;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;

import java.util.*;

public class Main {
    //输出excel 会自动创建
    private static String path = "D:\\desktop\\01风场\\一次调频\\X西六家子\\20240526";
    private static String outputName = "西六家子3.xlsx";
    private static String logFileName = "D:\\desktop\\01风场\\一次调频\\X西六家子\\20240526\\sanywind.trace.2024-05-26.{}.log";

    private static String startTimeStr = "2024-05-26 16:36:00.190";
    private static String endTimeStr = "2024-05-26 16:37:00.190";

    private static String logNums = "39-39";
    private static Map<String, Integer> turbineMeasurementMap = new HashMap<>();

    private static String turbineMeasurementNames = "时间,风机,风机正常,维护,能量管理平台停机指令,通讯中断,风机运行状态,电网电压" +
            ",电网电流,有功功率,无功功率,无功控制显示,风速,功率因数,当前桨叶角度,限功率百分比显示,发电量,发电机转速" +
            ",风向,油温,机舱温度,室外温度,轴1桨叶实际角度,轴2桨叶实际角度,EMS限功率百分比,算法状态机,理论功率返回,额定功率" +
            ",最小桨距角,可利用号,算法风向偏差大标志,算法振动大标志,低穿标志" +
            ",高穿标志,无功降容,净有功,blank,blank,blank,blank,blank,blank,下调速度,有功指令参考点,上调速度" +
            ",理论功率";

    private static String gridReturnNames = "时间,标杆风机数量,风场风机故障数量,标杆风机并网数量,标杆风机总有功,可控风机数量,限电停机数量," +
            "可控风机并网数量,可控风机实发总有功,可控风机理论有功,开机容量,风场通讯中断风机数量,风场并网发电风机算量,风场开机风机数量,风力理论有功（机舱风速法）," +
            "风场理论有功2（样板机法）,风场可用有功（机舱风速法）,风场可用有功（样板机法）,场内受阻电力（机舱风速法）,场内受阻电力（样板机法）,场外受阻电力（机舱风速法）," +
            "场外受阻电力（样板机法）,风场实发总有功,标杆风机容量,有功控制偏差,有功指令反馈,待风风机数,自由发电数,风场平均风速,运行风机平均功率,待机容量,发电容量,故障容量," +
            "停机容量,限功率容量,自由发电容量,停机数量,限功率数量,实际下发的指令," +
            "平均线损,反馈一次调频指令,反馈一次调频使能,检修台数,检修容量,可发有功上限,可发有功下限,有功投入,并网点有功反馈,限电标志位," +
            "限电量,EMSVersion算法版本号,变化率状态码,备用请求使能,备用请求码";

    static {
        String[] split = turbineMeasurementNames.split("\\,");
        for (int i = 0; i < split.length; i++) {
            turbineMeasurementMap.put(split[i], i - 2);
        }
    }

    private static DateTime startDateTime;
    private static DateTime endDateTime;

    public static void main(String[] args) {

        final TimeInterval timer = DateUtil.timer();
        //输出excel 会自动创建
        String outputFileName = path + "\\" + outputName;
        //删除上次输出的文件
        FileUtil.del(outputFileName);
        //日志文件路径
        BigExcelWriter writer = ExcelUtil.getBigWriter(outputFileName,"turbineMeasurements");
        //2024-05-17 12:04:08.276

        startDateTime = DateUtil.parse(startTimeStr, "yyyy-MM-dd HH:mm:ss.SSS");
        endDateTime = DateUtil.parse(endTimeStr, "yyyy-MM-dd HH:mm:ss.SSS");


        //日志起始页
        int startNumber = Convert.toInt(logNums.split("\\-")[0]);
        //日志结束页
        int endNumber = Convert.toInt(logNums.split("\\-")[logNums.split("\\-").length - 1]);

        ArrayList<String> turbineList = new ArrayList<>();
        ArrayList<String> gridReturnList = new ArrayList<>();
        ArrayList<String> theoryPowerList = new ArrayList<>();

        // 筛选关键字
        for (int i = startNumber; i <= endNumber; i++) {
            String filename1 = StrUtil.format(logFileName, i);
            FileReader fileReader1 = new FileReader(filename1);
            List<String> strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("有功传入 turbineMeasurements"));
            turbineList.addAll(strings1);
            strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("有功返回 gridReturnValues"));
            gridReturnList.addAll(strings1);
            strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("有功返回 theoryPower"));
            theoryPowerList.addAll(strings1);
        }
        int turbineNums = theoryPowerList.get(0).split("Power:")[1].split(",").length;
        // 单机数据
        writeTurbines(turbineList, theoryPowerList, writer);

        // 全场数据
        writer.setSheet("gridReturnValues");
        writeGridReturn(writer, gridReturnList);

        // 一次调频数据
        writer.setSheet("primaryFrequency");
        writePFC(writer, gridReturnList);

        // 单机合并数据(有功功率, 功率参考点)
        writer.setSheet("turbinesLong");
        writeTurbinesLong(writer, turbineList, turbineNums);


        //关闭writer，释放内存
        turbineList.clear();
        gridReturnList.clear();
        writer.close();
        Console.log("用时:" + timer.interval() + "ms...");
    }

    private static void writeTurbinesLong(BigExcelWriter writer, ArrayList<String> turbineList, int turbineSize) {
        writer.setFreezePane(1);
        List<List<Object>> rows = CollUtil.newArrayList();
        // 准备表头
//        String[] titleBase = {"有功功率", "有功指令参考点", "当前桨叶角度", "EMS限功率百分比", "发电机转速"};
        String[] titleBase = {"有功功率", "有功指令参考点"};
//        int[] titleIndexs = {7, 41, 12, 22};
        int[] titleIndexs = new int[titleBase.length];
        for (int i = 0; i < titleIndexs.length; i++) {
            titleIndexs[i] = turbineMeasurementMap.get(titleBase[i]);
        }
        double[] coeff = {1, 0.001};
        List<Object> titleList = new ArrayList<>();
        titleList.add("时间");
        for (int i = 0; i < turbineSize; i++) {
            for (String base : titleBase) {
                titleList.add(i + 1 + "-" + base);
            }
        }
        rows.add(titleList);
        // 准备数据
        Console.log("准备单机长数据...");
        for (int i = 0; i < turbineList.size(); i++) {
            ArrayList<Object> info = CollUtil.newArrayList();
            String time = getTime(turbineList.get(i));
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            info.add(time);
            String turbineInfo = turbineList.get(i).split("有功传入 turbineMeasurements:")[1];
            JSONArray objects = new JSONArray(turbineInfo);
            for (int turbIndex = 0; turbIndex < objects.size(); turbIndex++) {
                JSONArray jsonArray = objects.getJSONArray(turbIndex);
                List<Double> turbInfo = jsonArray.toList(Double.class);
                for (int i1 = 0; i1 < titleIndexs.length; i1++) {
                    int titleIndex = titleIndexs[i1];
                    info.add(turbInfo.get(titleIndex) * coeff[i1]);
                }
            }
            rows.add(info);
        }

        //一次性写出内容，强制输出标题
        writer.write(rows);
        rows = null;
        Console.log("写入准备单机长数据成功...");

    }

    private static void writePFC(BigExcelWriter writer, ArrayList<String> gridReturnList) {
        writer.setFreezePane(1);
        Console.log("pfc working...");
        String title = "时间,实际下发指令,风场实发总有功,并网点有功反馈,平均线损反馈,一次调频指令反馈,一次调频使能,风场可用有功";
        int[] indexs = {37, 21, 46, 38, 39, 40, 15};
        List<List<Object>> rows = CollUtil.newArrayList();
        rows.add(new ArrayList<>(Arrays.asList(title.split(","))));
        writer.autoSizeColumnAll();
        for (String gridReturn : gridReturnList) {
            String time = getTime(gridReturn);
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            String gridReturnMeasurements = gridReturn.split("有功返回 gridReturnValues:")[1];
            final JSONArray jsonArray = new JSONArray(gridReturnMeasurements);
            List<Double> doubles = jsonArray.toList(Double.class);
            ArrayList<Object> objects1 = new ArrayList<>();
            objects1.add(time);
            for (int index : indexs) {
                objects1.add(doubles.get(index));
            }
            rows.add(objects1);
        }

        Console.log("准备快频数据...");
        //一次性写出内容，强制输出标题
        writer.write(rows);
        rows = null;
        Console.log("写入快频数据成功...");
    }

    private static void writeGridReturn(BigExcelWriter writer, ArrayList<String> gridReturnList) {
        writer.setFreezePane(1);
        Console.log("gridReturnList.size()");
        Console.log(gridReturnList.size());
        String GridReturntitle2 = gridReturnNames;
        List<List<Object>> rows = CollUtil.newArrayList();
        rows.add(new ArrayList<>(Arrays.asList(GridReturntitle2.split("\\,"))));
        writer.autoSizeColumnAll();
        for (String gridReturn : gridReturnList) {
            String time = getTime(gridReturn);
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            String gridReturnMeasurements = gridReturn.split("有功返回 gridReturnValues:")[1];
            final JSONArray jsonArray = new JSONArray(gridReturnMeasurements);
            List<Double> doubles = jsonArray.toList(Double.class);
            ArrayList<Object> objects1 = new ArrayList<>();
            objects1.add(time);
            objects1.addAll(doubles);
            rows.add(objects1);
        }

        Console.log("准备写入全场数据...");
        //一次性写出内容，强制输出标题
        writer.write(rows);
        rows = null;
        Console.log("写入全场数据成功...");
    }

    private static void writeTurbines(ArrayList<String> turbineList, ArrayList<String> theoryPowerList, BigExcelWriter writer) {
        writer.setFreezePane(1);
        Console.log("turbineList.size()");
        Console.log(turbineList.size());
        List<List<Object>> rows = CollUtil.newArrayList();
        String turbineMeasurementsTitle = turbineMeasurementNames;
        final String[] split = turbineMeasurementsTitle.split("\\,");
        rows.add(new ArrayList<>(Arrays.asList(split)));
        for (int j = 0; j < turbineList.size() - 1; j++) {
            String turbineInfo = turbineList.get(j);
            String theoryPowerArrStr = theoryPowerList.get(j);
            String turbineMeasurements = turbineInfo.split("有功传入 turbineMeasurements:")[1];
            String theoryPowers = theoryPowerArrStr.split("Power:")[1];
            String time = getTime(turbineInfo);
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            JSONArray objects = new JSONArray(turbineMeasurements);
            JSONArray objects2 = new JSONArray(theoryPowers);
            for (int i = 0; i < objects.size(); i++) {
                JSONArray jsonArray = objects.getJSONArray(i);
                double theoryPower = Convert.toDouble(objects2.get(i));
                List<Double> doubles = jsonArray.toList(Double.class);
                ArrayList<Object> objects1 = new ArrayList<>();

                objects1.add(time);
                objects1.add(i + 1);
                objects1.addAll(doubles);
                objects1.add(theoryPower);
                rows.add(objects1);
            }
        }
        Console.log("准备写入单机数据...");

        //一次性写出内容，强制输出标题
        writer.write(rows);
        rows = null;
        Console.log("写入单机数据成功...");
    }


    private static String getTime(String input){
        // 提取日期的起始和结束位置
        int startIndex = input.indexOf("[") + 1; // 起始位置为[后的一个字符
        int endIndex = input.indexOf("]"); // 结束位置为]

        // 使用substring方法提取日期
        String date = input.substring(startIndex, endIndex);
        return date;
    }

}