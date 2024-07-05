package org.example;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.collection.ListUtil;
import cn.hutool.core.convert.Convert;
import cn.hutool.core.date.DateTime;
import cn.hutool.core.date.DateUtil;
import cn.hutool.core.io.FileUtil;
import cn.hutool.core.io.file.FileReader;
import cn.hutool.core.lang.Console;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.poi.excel.BigExcelWriter;
import cn.hutool.poi.excel.ExcelUtil;
import org.apache.poi.ss.usermodel.Sheet;
import org.example.util.MyUtil;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @description: agvc日志解析
 * @author: swen
 * @create: 2024/5/20 12:42
 */
public class AGVC {
    private String path = "D:\\desktop\\01风场\\KWE";
    private String outputName = "agvc.xlsx";
    private String logFileName = "sanywind.trace.2024-05-21.{}.log";
    private String logNums = "1-2";

    private  String startTimeStr = "2024-05-20 23:45:37.190";
    private  String endTimeStr = "2024-05-21 23:59:37.190";

    private  DateTime startDateTime;
    private  DateTime endDateTime;

    BigExcelWriter writer;
    ArrayList<String> avcTimeList = new ArrayList<>();
    ArrayList<String> avcAlgorithmReturnInfoList = new ArrayList<>();
    ArrayList<String> avcOrderList = new ArrayList<>();
    ArrayList<String> svgInfoList = new ArrayList<>();
    ArrayList<String> svgOrderList = new ArrayList<>();
    ArrayList<String> emsInfoList = new ArrayList<>();
    public void parseLog() {

        startDateTime = DateUtil.parse(startTimeStr, "yyyy-MM-dd HH:mm:ss.SSS");
        endDateTime = DateUtil.parse(endTimeStr, "yyyy-MM-dd HH:mm:ss.SSS");
        logFileName = path + "\\" + logFileName;
        //输出excel 会自动创建
        String dateStr = MyUtil.getDate(logFileName);
        String outputFileName = path + "\\" + dateStr + "-" + outputName;
        //删除上次输出的文件
        FileUtil.del(outputFileName);
        //日志文件路径
        writer = ExcelUtil.getBigWriter(outputFileName,"avcReturn");
        //日志起始页
        int startNumber = Convert.toInt(logNums.split("\\-")[0]);
        //日志结束页
        int endNumber = Convert.toInt(logNums.split("\\-")[1]);

        // 筛选关键字
        for (int i = startNumber; i <= endNumber; i++) {
            String filename1 = StrUtil.format(logFileName, i);
            FileReader fileReader1 = new FileReader(filename1);
            List<String> strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("无功传入 algorithmParamAVC:"));
            avcTimeList.addAll(strings1);
            strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("avcAlgorithmReturnInfo"));
            avcAlgorithmReturnInfoList.addAll(strings1);
            strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("avcOrder"));
            avcOrderList.addAll(strings1);
            strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("svgInfo"));
            svgInfoList.addAll(strings1);
            strings1 = fileReader1.readLines();
            strings1.removeIf(s -> !s.contains("svgOrder"));
            svgOrderList.addAll(strings1);
        }

        // 解析avc反馈
        parseAVCReturn();
        // 解析AVC指令
        writer.setSheet("avcOrder");
        parseAVCOrder();
        // 解析svg入参
        writer.setSheet("svgInfo");
        parseSVGInfoAndOrder();
        // 结束
        avcAlgorithmReturnInfoList.clear();
        avcOrderList.clear();
        writer.close();
    }

    private void parseSVGInfoAndOrder() {
        Console.log("parseSVGInfoAndOrder start...");
        // 处理表头
        List<List<Object>> rows = CollUtil.newArrayList(); //所有数据
        writer.setFreezePane(1);
        String title1 = "实发无功,指令反馈,最大可发无功,最大可吸无功,当前运行模式,通信中断信号,svg投退,运行/故障";
        final String[] split = title1.split("\\,");
        List<Object> titleList = new ArrayList<>();
        titleList.add("时间");
        int svgCount = StrUtil.count(svgOrderList.get(0), ",");
        for (int i = 0; i < svgCount; i++) {
            for (String s : split) {
                titleList.add(i + s);
            }
            titleList.add(i + "svg指令");
        }
        rows.add(titleList);
        // 处理数据
        for (int i = 0; i < svgInfoList.size(); i++) {
            String svgInfoStr = svgInfoList.get(i);
            String svgOrder = svgOrderList.get(i); // "svgOrder" : [ 1.0E9, 1.0E9 ],
            ArrayList<Object> row = CollUtil.newArrayList();
            String time = MyUtil.getTimeAGVC(avcTimeList.get(i));
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            row.add(time);
            String info = svgInfoStr.split("\"svgInfo\" : ")[1];
            String order = svgOrder.split("\"svgOrder\" : ")[1];
            JSONArray jsonArray = new JSONArray(info);
            JSONArray jsonArray2 = new JSONArray(order);
            for (int i1 = 0; i1 < jsonArray.size(); i1++) {
                JSONArray svgInfo = jsonArray.getJSONArray(i1);
                List<Double> list = svgInfo.toList(Double.class);
                List<Double> list1 = jsonArray2.toList(Double.class);
                row.addAll(list);
                row.add(list1.get(i1));
            }
            rows.add(row);
        }
        writer.write(rows);
        rows = null;
        Console.log("parseSVGInfoAndOrder end...");
    }

    private void parseAVCOrder() {
        Console.log("parseAVCOrder start...");
        // 处理第一行
        writer.setFreezePane(1);
        String title1 = "时间,系统投切,指令类型,指令值,开闭控制模式,控制模式,并网点无功功率,并网点Uab," +
                "并网点Ubc,并网点Uca,并网点异常信号,母线参考电压偏差,当前主变分接头档位,并网点功率因数,并网点有功功率,无功值指令,电压指令,功率因数指令";
        final String[] split = title1.split("\\,");
        List<List<Object>> rows = CollUtil.newArrayList();
        rows.add(new ArrayList<>(Arrays.asList(split)));
        Sheet sheet = writer.getSheet();
        // 填充数据
        for (int i = 0; i < avcOrderList.size(); i++) {
            String avcAlgorithmReturnInfo = avcOrderList.get(i);
            ArrayList<Object> row = CollUtil.newArrayList();
            String info = avcAlgorithmReturnInfo.split("\"avcOrder\" :")[1];
            JSONArray jsonArray = new JSONArray(info);
            List<Double> list = jsonArray.toList(Double.class);
            String time = MyUtil.getTimeAGVC(avcTimeList.get(i));
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            row.add(time);
            row.addAll(list);
            rows.add(row);
        }
        writer.write(rows);
        rows = null;
        Console.log("parseAVCOrder end...");
    }

    private void parseAVCReturn() {
        Console.log("parseAVCReturn start...");
        // 处理第一行
        writer.setFreezePane(1);
        String title1 = "时间,无功控制算法版本,系统投入,系统开闭模式,无功调控模式,无功指令类型,AVC指令反馈,并网点电压,并网点无功功率," +
                "增闭锁信号,减闭锁信号,可增无功容量,可减无功容量,SVG控制模式反馈,控制变化步长反馈,控制死区反馈,分配策略,主变档位反馈,并网点通信," +
                "并网点无功功率超目标上限,并网点电压超目标电压上限,风场风机总无功,风机可调无功上限值,风机可调无功下限值,风场理论总无功,SVG可调无功上限," +
                "SVG可调无功下限,并网点功率因数,风场实发总无功反馈,风场无功理论上限," +
                "风场无功理论下限,系统阻抗,无功指令反馈,电压指令反馈,功率因数指令反馈,无功线损,并网点使能,电压死区,SVG总无功,无功状态码,实际下发指令,平均电压";
        final String[] split = title1.split("\\,");
        List<List<Object>> rows = CollUtil.newArrayList();
        rows.add(new ArrayList<>(Arrays.asList(split)));
        // 填充数据
        for (int i = 0; i < avcAlgorithmReturnInfoList.size(); i++) {
            String avcAlgorithmReturnInfo = avcAlgorithmReturnInfoList.get(i);
            ArrayList<Object> row = CollUtil.newArrayList();
            String info = avcAlgorithmReturnInfo.split("\"avcAlgorithmReturnInfo\" : ")[1];
            JSONArray jsonArray = new JSONArray(info);
            List<Double> list = jsonArray.toList(Double.class);
            String time = MyUtil.getTimeAGVC(avcTimeList.get(i));
            DateTime dateTime = DateUtil.parseDateTime(time);
            if (dateTime.isAfter(endDateTime) || dateTime.isBefore(startDateTime)) {
                continue;
            }
            row.add(time);
            row.addAll(list);
            rows.add(row);
        }
        writer.write(rows);
        rows = null;
        Console.log("parseAVCReturn end...");
    }



    public static void main(String[] args) {
        new AGVC().parseLog();
    }
}
