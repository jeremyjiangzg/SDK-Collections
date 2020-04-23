package com.asizon.extractdate;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class AutoExtractTime {
    private final String MD_HM = "MM月dd日 HH:mm";
    private final String YMD_HMS = "yyyy-MM-dd HH:mm:ss";

    //提取月正则
    private final String monthRegEx = "((\\d{1,2}|[一二三四五六七八九十][一二]?)[月])";
    //提取日正则
    private final String dayRegEx = "((\\d{1,2}|[一二三四五六七八九十][一二]?)[号日])";
    //提取中文月或日正则
    private final String chineseNumRegEx = "([一二三四五六七八九十][一二]?)";
    //提取时分正则
    private final String hmRegEx = "((([上下]午)?|([早晚]上?))?(\\d{1,2}|[一二三四五六七八九十][一二]?)[:点](\\d{1,2})?)";
    //提取上下午关键字正则
    private final String keyRegEx = "(([上下]午)|([早晚]上?))";
    //提取阿拉伯数字时分正则
    private final String numberRegEx = "(\\d{1,2})";
    //提取中文时分正则
    private final String chineseRegEx = "([一二三四五六七八九十][一二]?[:点](\\d{1,2})?)";

    private String[] chineseArr = new String[]{"一", "二", "三", "四", "五", "六", "七", "八", "九", "十", "十一", "十二"};

    /**
     * 是否开启功能
     */
    private boolean isEnable = true;
    /**
     * 上限限制时间
     */
    private Date startLimitDate = null;
    /**
     * 下限限制时间
     */
    private Date endLimitDate = null;

    public AutoExtractTime() {

    }

    /**
     * 有效时间范围天数
     *
     * @param dayRange
     */
    public AutoExtractTime(int dayRange) {
        this.startLimitDate = new Date();
        this.endLimitDate = getOffsetDate(startLimitDate, dayRange);
    }

    /**
     * @param startLimitDate 有效起始日期
     */
    public AutoExtractTime(Date startLimitDate) {
        this.startLimitDate = startLimitDate;
    }

    /**
     * @param startLimitDate
     * @param dayCount
     */
    public AutoExtractTime(Date startLimitDate, int dayCount) {
        this.startLimitDate = startLimitDate;
        this.endLimitDate = getOffsetDate(startLimitDate, dayCount);
    }

    /**
     * @param startLimitDate
     * @param endLimitDate
     */
    public AutoExtractTime(Date startLimitDate, Date endLimitDate) {
        this.startLimitDate = startLimitDate;
        this.endLimitDate = endLimitDate;
    }

    /**
     * 计算当前日期前后偏移offsetDay天的日期，正数往后推，负数往前推
     *
     * @param currentDate
     * @param offsetDay
     * @return
     */
    private static Date getOffsetDate(Date currentDate, int offsetDay) {
        Date date = new Date(currentDate.getTime());// 取时间
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, offsetDay);
        return calendar.getTime();
    }

    public synchronized TimeMode start(String content) {
        if (isNullOrEmpty(content)) {
            return null;
        }
        if (!isEnable) {
            return null;
        }
        isEnable = false;
        TimeMode timeMode = fetchStandardTimeFromText(content);
        if (!timeMode.isSuccessful()) {
            //实例只提取一次
            isEnable = true;
        }
        return timeMode;
    }

    private TimeMode fetchStandardTimeFromText(String content) {
        Pattern pMonth = Pattern.compile(monthRegEx);
        Pattern pDay = Pattern.compile(dayRegEx);
        Pattern pHour = Pattern.compile(hmRegEx);

        TimeMode timeMode = new TimeMode();
        //提取段落中时间信息(月、日、时分)
        Matcher mHm = pHour.matcher(content);
        if (mHm.find()) {
            timeMode.setHm(mHm.group());
        } else {
            timeMode.setSuccessful(false);
            return timeMode;
        }

        Matcher mMonth = pMonth.matcher(content);
        if (mMonth.find()) {
            timeMode.setMonth(mMonth.group());
        }

        Matcher mDay = pDay.matcher(content);
        if (mDay.find()) {
            timeMode.setDay(mDay.group());
        }

        timeMode.setSuccessful(generateTimestamp(timeMode));
        return timeMode;
    }

    /**
     * 将提取的时间模型生成时间戳timestamp
     *
     * @return false:失败，true:成功
     */
    private boolean generateTimestamp(TimeMode timeMode) {
        if (!extractHm(timeMode)) {
            return false;
        }
        if (!extractMonth(timeMode)) {
            return false;
        }
        if (!extractDay(timeMode)) {
            return false;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(getCurrentYear());
        builder.append("-");
        builder.append(timeMode.getMonth());
        builder.append("-");
        builder.append(timeMode.getDay());
        builder.append(" ");
        builder.append(timeMode.getHour());
        builder.append(":");
        builder.append(timeMode.getMinute());
        builder.append(":");
        builder.append("00");

        String timeFormatValue = builder.toString();
        timeMode.setTimestamp(formatToLong(timeFormatValue));
        timeMode.setFormat(longToFormat(timeMode.getTimestamp(), MD_HM));

        return checkTimeValid(timeMode.getTimestamp());
    }

    private boolean checkTimeValid(long time) {
        if (startLimitDate == null && endLimitDate == null) {
            return true;
        }
        if (startLimitDate != null && time < startLimitDate.getTime()) {
            return false;
        }
        return endLimitDate == null || time <= endLimitDate.getTime();
    }

    /**
     * 检查月份是否有效
     *
     * @param month 格式为"08"、"12"
     * @return true:月份有效，false:月份无效
     */
    private boolean isMonthValid(String month) {
        if (isNullOrEmpty(month)) {
            return false;
        }
        return month.compareTo("12") <= 0;
    }

    /**
     * 检查日是否有效
     *
     * @param day 格式为"08"、"31"
     * @return true:日有效，false:日无效
     */
    private boolean isDayValid(String day) {
        if (isNullOrEmpty(day)) {
            return false;
        }
        return day.compareTo("31") <= 0;
    }

    /**
     * 检查小时是否有效
     *
     * @param hour 格式为"08"、"24"
     * @return true:小时有效，false:小时无效
     */
    private boolean isHourValid(String hour) {
        if (isNullOrEmpty(hour)) {
            return false;
        }
        return hour.compareTo("24") <= 0;
    }

    /**
     * 检查分钟是否有效
     *
     * @param minute 格式为"08"、"60"
     * @return true:分钟有效，false:分钟无效
     */
    private boolean isMinuteValid(String minute) {
        if (isNullOrEmpty(minute)) {
            return false;
        }
        return minute.compareTo("60") <= 0;
    }

    /**
     * 处理时间模型月份
     *
     * @return false:失败，true:成功
     */
    private boolean extractMonth(TimeMode timeMode) {
        if (isNullOrEmpty(timeMode.getMonth())) {
            timeMode.setMonth(String.valueOf(getCurrentMonth()));
        } else {
            timeMode.setMonth(timeMode.getMonth().substring(0, timeMode.getMonth().indexOf("月")));

            Pattern pattern = Pattern.compile(numberRegEx);
            Matcher matcher = pattern.matcher(timeMode.getMonth());
            if (matcher.find()) {
                timeMode.setMonth(matcher.group());
            } else {
                Pattern pChinese = Pattern.compile(chineseNumRegEx);
                Matcher mChinese = pChinese.matcher(timeMode.getMonth());
                if (mChinese.find()) {
                    int result = chineseNumToArabicNum(mChinese.group());
                    if (result > 0) {
                        timeMode.setMonth(String.valueOf(result));
                    } else {
                        timeMode.setMonth(String.valueOf(getCurrentMonth()));
                    }
                } else {
                    timeMode.setMonth(String.valueOf(getCurrentMonth()));
                }
            }
        }

        timeMode.setMonth(appendZero(timeMode.getMonth()));
        return isMonthValid(timeMode.getMonth());
    }

    /**
     * 处理时间模型日
     *
     * @return false:失败，true:成功
     */
    private boolean extractDay(TimeMode timeMode) {
        if (isNullOrEmpty(timeMode.getDay())) {
            timeMode.setDay(String.valueOf(getCurrentDay()));
        } else {
            Pattern pattern = Pattern.compile(numberRegEx);
            Matcher matcher = pattern.matcher(timeMode.getDay());
            if (matcher.find()) {
                timeMode.setDay(matcher.group());
            } else {
                Pattern pChinese = Pattern.compile(chineseNumRegEx);
                Matcher mChinese = pChinese.matcher(timeMode.getDay());
                if (mChinese.find()) {
                    int result = chineseNumToArabicNum(mChinese.group());
                    if (result > 0) {
                        timeMode.setDay(String.valueOf(result));
                    } else {
                        timeMode.setDay(String.valueOf(getCurrentDay()));
                    }
                } else {
                    timeMode.setDay(String.valueOf(getCurrentDay()));
                }
            }
        }
        timeMode.setDay(appendZero(timeMode.getDay()));
        return isDayValid(timeMode.getDay());
    }

    /**
     * 提取时分
     *
     * @return true:提取成功，false:提取失败
     */
    private boolean extractHm(TimeMode timeMode) {
        if (isNullOrEmpty(timeMode.getHm())) {
            return false;
        }

        //提取上下午关键字
        String keyValue = "";
        Pattern pNoon = Pattern.compile(keyRegEx);
        Matcher mNoon = pNoon.matcher(timeMode.getHm());
        if (mNoon.find()) {
            keyValue = mNoon.group();
        }

        //提取时钟值
        int hourValue = 0;
        int minuteValue = 0;
        Pattern pattern = Pattern.compile(numberRegEx);
        Matcher matcher = pattern.matcher(timeMode.getHm());
        if (matcher.find()) {
            try {
                if (timeMode.getHm().contains(":")) {
                    String[] arr = timeMode.getHm().split(":");
                    if (arr.length < 2) {
                        return false;
                    }
                    hourValue = Integer.parseInt(arr[0]);
                    minuteValue = Integer.parseInt(arr[1]);
                } else {
                    hourValue = Integer.parseInt(matcher.group());
                }
            } catch (Exception e) {
                return false;
            }
        } else {
            Pattern pChinese = Pattern.compile(chineseRegEx);
            Matcher mChinese = pChinese.matcher(timeMode.getHm());
            if (mChinese.find()) {
                String chineseValue = mChinese.group();
                if (chineseValue.contains("点")) {
                    String[] arr = chineseValue.split("点");
                    hourValue = chineseNumToArabicNum(arr[0]);
                    if (hourValue < 0) {
                        return false;
                    }
                }
            }
        }
        if ((keyValue.contains("下午") | keyValue.contains("晚")) && hourValue < 12) {
            timeMode.setHour(String.valueOf(hourValue + 12));
        } else {
            timeMode.setHour(appendZero(String.valueOf(hourValue)));
        }
        timeMode.setMinute(appendZero(String.valueOf(minuteValue)));
        return isHourValid(timeMode.getHour()) && isMinuteValid(timeMode.getMinute());
    }

    /**
     * 将阿拉伯数字1-12转化成中文一、二...十一、十二
     */
    private int chineseNumToArabicNum(String chineseNum) {
        if (isNullOrEmpty(chineseNum)) {
            return -1;
        }
        for (int i = 0; i < chineseArr.length; i++) {
            if (equals(chineseNum, chineseArr[i])) {
                return ++i;
            }
        }
        return -1;
    }

    /**
     * 时间日期位数不足，在前面补"0"
     */
    private String appendZero(String value) {
        int result;
        try {
            result = Integer.valueOf(value);
        } catch (NumberFormatException e) {
            return "00";
        }

        if (result < 0) {
            return "00";
        }
        if (result < 10) {
            return "0" + result;
        } else {
            return String.valueOf(result);
        }
    }

    private long formatToLong(String format) {
        SimpleDateFormat sdf = new SimpleDateFormat(YMD_HMS);
        try {
            Date date = sdf.parse(format);
            return date.getTime();
        } catch (ParseException e) {
            return 0L;
        }
    }

    private String longToFormat(Long timestamp, String dateFormat) {
        if (isNullOrEmpty(dateFormat)) {
            dateFormat = MD_HM;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
        return sdf.format(new Date(timestamp));
    }

    /**
     * 获取当前年份
     */
    private int getCurrentYear() {
        return Calendar.getInstance().get(Calendar.YEAR);
    }

    /**
     * 获取当前月份
     */
    private int getCurrentMonth() {
        return Calendar.getInstance().get(Calendar.MONTH) + 1;
    }

    /**
     * 获取当日
     */
    private int getCurrentDay() {
        return Calendar.getInstance().get(Calendar.DATE);
    }

    private boolean isNullOrEmpty(final CharSequence value) {
        return value == null || value.length() == 0;
    }

    private boolean equals(final String s1, final String s2) {
        return (s1 == null && s2 == null) || (s1 != null && s2 != null && s1.equals(s2));
    }
}
