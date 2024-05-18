package org.example;


import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.date.TimeInterval;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        final TimeInterval timer = DateUtil.timer();

        //输出excel 会自动创建
        String path = "D:\\desktop\\01风场\\一次调频\\鸡冠山\\2024-05-17";
        String outputFileName = path + "\\" + "writeTest4.xlsx";
        //删除上次输出的文件
        FileUtil.del(outputFileName);
        //日志文件路径
        String logFileName = "D:\\desktop\\01风场\\一次调频\\鸡冠山\\2024-05-17\\sanywind.trace.2024-05-17.{}.log";
        //默认UTF-8编码，可以在构造中传入第二个参数做为编码

        BigExcelWriter writer = ExcelUtil.getBigWriter(outputFileName,"turbineMeasurements");

        String logNums = "27-28";
        //日志起始页
        int startNumber = Convert.toInt(logNums.split("\\-")[0]);
        //日志结束页
        int endNumber = Convert.toInt(logNums.split("\\-")[1]);

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
        writer.setSheet("turbinesLong2");
        writeTurbinesLong2(writer, turbineList, turbineNums);

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
//        String[] titleBase = {"有功功率", "功率参考点", "桨叶角度"};
//        int[] titleIndexs = {7, 41, 12};
//        double[] coeff = {1, 0.001, 1};
        String[] titleBase = {"有功功率"};
        int[] titleIndexs = {7};
        double[] coeff = {1};
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
            info.add(getTime(turbineList.get(i)));
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

    private static void writeTurbinesLong2(BigExcelWriter writer, ArrayList<String> turbineList, int turbineSize) {
        writer.setFreezePane(1);
        List<List<Object>> rows = CollUtil.newArrayList();
        // 准备表头
//        String[] titleBase = {"有功功率", "功率参考点", "桨叶角度"};
//        int[] titleIndexs = {7, 41, 12};
//        double[] coeff = {1, 0.001, 1};
        String[] titleBase = {"状态机"};
        int[] titleIndexs = {23};
        double[] coeff = {1};
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
            info.add(getTime(turbineList.get(i)));
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
        String GridReturntitle2 = "时间\t 标杆风机数量\t 风场风机故障数量\t 标杆风机并网数量\t 标杆风机总有功\t " +
                "可控风机数量\t 限电停机数量\t 可控风机并网数量\t 可控风机实发总有功\t 可控风机理论有功\t 开机容量\t " +
                "风场通讯中断风机数量\t 风场并网发电风机算量\t 风场开机风机数量\t 风力理论有功（机舱风速法）\t 风场理论有功2（样板机法）\t " +
                "风场可用有功（机舱风速法）\t 风场可用有功（样板机法）\t 场内受阻电力（机舱风速法）\t 场内受阻电力（样板机法）\t " +
                "场外受阻电力（机舱风速法）\t 场外受阻电力（样板机法）\t 风场实发总有功\t 标杆风机容量\t 有功控制偏差\t 有功指令反馈\t " +
                "待风风机数\t 自由发电数\t 风场平均风速\t 运行风机平均功率\t 待机容量\t 发电容量\t 故障容量\t 停机容量\t 限功率容量\t " +
                "自由发电容量\t 停机数量\t 限功率数量\t 实际下发的指令\t 平均线损\t反馈一次调频指令\t反馈一次调频使能\t检修台数\t " +
                "检修容量\t可发有功上限\t可发有功下限\t有功投入\t并网点有功反馈\t限电标志位\t限电量\tEMSVersion算法版本号\t" +
                "变化率状态码\t 备用请求使能\t 备用请求码";
        List<List<Object>> rows = CollUtil.newArrayList();
        rows.add(new ArrayList<>(Arrays.asList(GridReturntitle2.split("\t"))));
        writer.autoSizeColumnAll();
        for (String gridReturn : gridReturnList) {
            String time = getTime(gridReturn);
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
        String turbineMeasurementsTitle = "时间\t风机\t风机正常\t维护\t能量管理平台停机指令\t通讯中断\t风机运行状态\t电网电压" +
                "\t电网电流\t有功功率\t无功功率\t无功控制显示\t风速\t功率因数\t当前桨叶角度\t限功率百分比显示\t发电量\t发电机转速" +
                "\t风向\t油温\t机舱温度\t室外温度\t轴1桨叶实际角度\t轴2桨叶实际角度\t限功率百分比\t算法状态机\t理论功率返回\t额定功率" +
                "\t最小桨距角\t可利用号\t算法风向偏差大标志\t算法振动大标志\t低穿标志" +
                "\t高穿标志\t无功降容\t净有功\tblank\tblank\tblank\tblank\tblank\tblank\t下调速度\t有功指令参考点\t上调速度" +
                "\t理论功率";
        final String[] split = turbineMeasurementsTitle.split("\t");
        rows.add(new ArrayList<>(Arrays.asList(split)));
        for (int j = 0; j < turbineList.size() - 1; j++) {
            String turbineInfo = turbineList.get(j);
            String theoryPowerArrStr = theoryPowerList.get(j);
            String turbineMeasurements = turbineInfo.split("有功传入 turbineMeasurements:")[1];
            String theoryPowers = theoryPowerArrStr.split("Power:")[1];
            String time = getTime(turbineInfo);
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