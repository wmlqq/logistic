package com.wmlqq.logistic.util;

import com.wmlqq.logistic.constant.LogisticsConstant;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * 日期时间工具类
 * @author wmlqq
 * @date 2024-05-01
 */
public class DateUtils {

    /**
     * 获取当前日期（yyyy-MM-dd）
     * @return 格式化后的日期字符串
     */
    public static String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat(LogisticsConstant.DATE_FORMAT_YYYY_MM_DD);
        return sdf.format(new Date());
    }

    /**
     * 获取当前时间（yyyy-MM-dd HH:mm:ss）
     * @return 格式化后的时间字符串
     */
    public static String getCurrentDateTime() {
        SimpleDateFormat sdf = new SimpleDateFormat(LogisticsConstant.DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
        return sdf.format(new Date());
    }

    /**
     * 字符串转Date（yyyy-MM-dd）
     * @param dateStr 日期字符串
     * @return Date对象
     * @throws ParseException 解析异常
     */
    public static Date parseDate(String dateStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(LogisticsConstant.DATE_FORMAT_YYYY_MM_DD);
        return sdf.parse(dateStr);
    }

    /**
     * 字符串转Date（yyyy-MM-dd HH:mm:ss）
     * @param dateTimeStr 时间字符串
     * @return Date对象
     * @throws ParseException 解析异常
     */
    public static Date parseDateTime(String dateTimeStr) throws ParseException {
        SimpleDateFormat sdf = new SimpleDateFormat(LogisticsConstant.DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
        return sdf.parse(dateTimeStr);
    }

    /**
     * Date转字符串（yyyy-MM-dd）
     * @param date Date对象
     * @return 格式化后的字符串
     */
    public static String formatDate(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(LogisticsConstant.DATE_FORMAT_YYYY_MM_DD);
        return sdf.format(date);
    }

    /**
     * Date转字符串（yyyy-MM-dd HH:mm:ss）
     * @param date Date对象
     * @return 格式化后的字符串
     */
    public static String formatDateTime(Date date) {
        if (date == null) {
            return null;
        }
        SimpleDateFormat sdf = new SimpleDateFormat(LogisticsConstant.DATE_FORMAT_YYYY_MM_DD_HH_MM_SS);
        return sdf.format(date);
    }

    /**
     * 获取指定日期的前n天
     * @param date 基准日期
     * @param days 天数（负数表示后n天）
     * @return 计算后的日期字符串（yyyy-MM-dd）
     * @throws ParseException 解析异常
     */
    public static String getBeforeDate(String date, int days) throws ParseException {
        Date parseDate = parseDate(date);
        Calendar calendar = new GregorianCalendar();
        calendar.setTime(parseDate);
        calendar.add(Calendar.DATE, -days);
        return formatDate(calendar.getTime());
    }

    /**
     * 获取指定日期的后n天
     * @param date 基准日期
     * @param days 天数
     * @return 计算后的日期字符串（yyyy-MM-dd）
     * @throws ParseException 解析异常
     */
    public static String getAfterDate(String date, int days) throws ParseException {
        return getBeforeDate(date, -days);
    }

    /**
     * 计算两个日期之间的天数差
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return 天数差（endDate - startDate）
     * @throws ParseException 解析异常
     */
    public static long getDaysBetween(String startDate, String endDate) throws ParseException {
        Date start = parseDate(startDate);
        Date end = parseDate(endDate);
        long diff = end.getTime() - start.getTime();
        return diff / (1000 * 60 * 60 * 24);
    }

    /**
     * 判断日期是否在指定区间内
     * @param targetDate 目标日期
     * @param startDate 开始日期
     * @param endDate 结束日期
     * @return true-在区间内，false-不在
     * @throws ParseException 解析异常
     */
    public static boolean isDateInRange(String targetDate, String startDate, String endDate) throws ParseException {
        Date target = parseDate(targetDate);
        Date start = parseDate(startDate);
        Date end = parseDate(endDate);
        return target.after(start) && target.before(end);
    }

    /**
     * 获取当月第一天
     * @return 当月第一天字符串（yyyy-MM-dd）
     */
    public static String getFirstDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, 1);
        return formatDate(calendar.getTime());
    }

    /**
     * 获取当月最后一天
     * @return 当月最后一天字符串（yyyy-MM-dd）
     */
    public static String getLastDayOfMonth() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.DAY_OF_MONTH, calendar.getActualMaximum(Calendar.DAY_OF_MONTH));
        return formatDate(calendar.getTime());
    }

    /**
     * 判断是否为闰年
     * @param year 年份
     * @return true-闰年，false-平年
     */
    public static boolean isLeapYear(int year) {
        return (year % 4 == 0 && year % 100 != 0) || (year % 400 == 0);
    }

    /**
     * 获取指定年份的天数
     * @param year 年份
     * @return 天数
     */
    public static int getDaysOfYear(int year) {
        return isLeapYear(year) ? 366 : 365;
    }

    /**
     * 时间戳转字符串（yyyy-MM-dd HH:mm:ss）
     * @param timestamp 时间戳（毫秒）
     * @return 格式化后的字符串
     */
    public static String timestampToDateTime(long timestamp) {
        Date date = new Date(timestamp);
        return formatDateTime(date);
    }

    /**
     * 字符串转时间戳（yyyy-MM-dd HH:mm:ss）
     * @param dateTimeStr 时间字符串
     * @return 时间戳（毫秒）
     * @throws ParseException 解析异常
     */
    public static long dateTimeToTimestamp(String dateTimeStr) throws ParseException {
        Date date = parseDateTime(dateTimeStr);
        return date.getTime();
    }
}


package com.wmlqq.logistic.util;

import org.apache.commons.lang3.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 参数校验工具类
 * @author wmlqq
 * @date 2024-05-01
 */
public class ValidateUtils {

    /**
     * 校验字符串是否为空
     * @param str 待校验字符串
     * @return true-空，false-非空
     */
    public static boolean isEmpty(String str) {
        return StringUtils.isEmpty(str);
    }

    /**
     * 校验字符串是否非空
     * @param str 待校验字符串
     * @return true-非空，false-空
     */
    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }

    /**
     * 校验字符串是否为空白（包含空格、制表符等）
     * @param str 待校验字符串
     * @return true-空白，false-非空白
     */
    public static boolean isBlank(String str) {
        return StringUtils.isBlank(str);
    }

    /**
     * 校验字符串是否非空白
     * @param str 待校验字符串
     * @return true-非空白，false-空白
     */
    public static boolean isNotBlank(String str) {
        return !isBlank(str);
    }

    /**
     * 校验手机号格式
     * @param phone 手机号
     * @return true-合法，false-非法
     */
    public static boolean isPhoneValid(String phone) {
        if (isEmpty(phone)) {
            return false;
        }
        // 中国大陆手机号正则
        String regex = "^1[3-9]\\d{9}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(phone);
        return matcher.matches();
    }

    /**
     * 校验身份证号格式（18位）
     * @param idCard 身份证号
     * @return true-合法，false-非法
     */
    public static boolean isIdCardValid(String idCard) {
        if (isEmpty(idCard) || idCard.length() != 18) {
            return false;
        }
        // 18位身份证正则（简化版）
        String regex = "^[1-9]\\d{5}(18|19|20)\\d{2}((0[1-9])|(1[0-2]))(([0-2][1-9])|10|20|30|31)\\d{3}[0-9Xx]$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(idCard);
        if (!matcher.matches()) {
            return false;
        }
        // 校验码验证（可选，此处简化）
        return true;
    }

    /**
     * 校验邮箱格式
     * @param email 邮箱
     * @return true-合法，false-非法
     */
    public static boolean isEmailValid(String email) {
        if (isEmpty(email)) {
            return false;
        }
        String regex = "^[a-zA-Z0-9_-]+@[a-zA-Z0-9_-]+(\\.[a-zA-Z0-9_-]+)+$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(email);
        return matcher.matches();
    }

    /**
     * 校验物流单号格式（示例：12位数字+字母组合）
     * @param logisticsNo 物流单号
     * @return true-合法，false-非法
     */
    public static boolean isLogisticsNoValid(String logisticsNo) {
        if (isEmpty(logisticsNo)) {
            return false;
        }
        String regex = "^[A-Za-z0-9]{12,20}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(logisticsNo);
        return matcher.matches();
    }

    /**
     * 校验订单号格式（示例：YYYYMMDD+8位数字）
     * @param orderNo 订单号
     * @return true-合法，false-非法
     */
    public static boolean isOrderNoValid(String orderNo) {
        if (isEmpty(orderNo) || orderNo.length() != 16) {
            return false;
        }
        String regex = "^\\d{8}\\d{8}$";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(orderNo);
        return matcher.matches();
    }

    /**
     * 校验数值是否为正整数
     * @param num 待校验数值
     * @return true-正整数，false-非正整数
     */
    public static boolean isPositiveInteger(Integer num) {
        return num != null && num > 0;
    }

    /**
     * 校验数值是否为非负整数
     * @param num 待校验数值
     * @return true-非负整数，false-负整数
     */
    public static boolean isNonNegativeInteger(Integer num) {
        return num != null && num >= 0;
    }

    /**
     * 校验分页参数
     * @param pageNum 页码
     * @param pageSize 页大小
     * @return true-合法，false-非法
     */
    public static boolean isPageParamValid(Integer pageNum, Integer pageSize) {
        return isPositiveInteger(pageNum) && isPositiveInteger(pageSize) && pageSize <= 100;
    }

    /**
     * 校验物流状态是否合法
     * @param status 物流状态
     * @return true-合法，false-非法
     */
    public static boolean isLogisticsStatusValid(String status) {
        if (isEmpty(status)) {
            return false;
        }
        return status.equals(com.wmlqq.logistic.constant.LogisticsConstant.ORDER_STATUS_PENDING)
                || status.equals(com.wmlqq.logistic.constant.LogisticsConstant.ORDER_STATUS_COLLECTED)
                || status.equals(com.wmlqq.logistic.constant.LogisticsConstant.ORDER_STATUS_TRANSPORT)
                || status.equals(com.wmlqq.logistic.constant.LogisticsConstant.ORDER_STATUS_DELIVERING)
                || status.equals(com.wmlqq.logistic.constant.LogisticsConstant.ORDER_STATUS_SIGNED)
                || status.equals(com.wmlqq.logistic.constant.LogisticsConstant.ORDER_STATUS_CANCELLED);
    }

    /**
     * 校验物流类型是否合法
     * @param type 物流类型
     * @return true-合法，false-非法
     */
    public static boolean isLogisticsTypeValid(String type) {
        if (isEmpty(type)) {
            return false;
        }
        return type.equals(com.wmlqq.logistic.constant.LogisticsConstant.LOGISTICS_TYPE_EXPRESS)
                || type.equals(com.wmlqq.logistic.constant.LogisticsConstant.LOGISTICS_TYPE_FREIGHT)
                || type.equals(com.wmlqq.logistic.constant.LogisticsConstant.LOGISTICS_TYPE_SPECIAL);
    }

    /**
     * 批量校验参数是否为空
     * @param params 参数数组
     * @return true-全部非空，false-存在空值
     */
    public static boolean checkAllNotEmpty(String... params) {
        if (params == null || params.length == 0) {
            return false;
        }
        for (String param : params) {
            if (isEmpty(param)) {
                return false;
            }
        }
        return true;
    }
}



package com.wmlqq.logistic.util;

import com.wmlqq.logistic.constant.LogisticsConstant;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * 响应结果工具类
 * @author wmlqq
 * @date 2024-05-01
 */
public class ResultUtils {

    /**
     * 通用响应结果类
     */
    public static class Result<T> implements Serializable {
        private static final long serialVersionUID = 1L;

        private Integer code;       // 响应码
        private String msg;         // 响应信息
        private T data;             // 响应数据
        private Long timestamp;     // 时间戳

        public Result() {
            this.timestamp = System.currentTimeMillis();
        }

        public Result(Integer code, String msg) {
            this();
            this.code = code;
            this.msg = msg;
        }

        public Result(Integer code, String msg, T data) {
            this(code, msg);
            this.data = data;
        }

        // Getter & Setter
        public Integer getCode() {
            return code;
        }

        public void setCode(Integer code) {
            this.code = code;
        }

        public String getMsg() {
            return msg;
        }

        public void setMsg(String msg) {
            this.msg = msg;
        }

        public T getData() {
            return data;
        }

        public void setData(T data) {
            this.data = data;
        }

        public Long getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Long timestamp) {
            this.timestamp = timestamp;
        }
    }

    /**
     * 成功响应（无数据）
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success() {
        return new Result<>(LogisticsConstant.SUCCESS_CODE, LogisticsConstant.SUCCESS_MSG);
    }

    /**
     * 成功响应（带数据）
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(T data) {
        return new Result<>(LogisticsConstant.SUCCESS_CODE, LogisticsConstant.SUCCESS_MSG, data);
    }

    /**
     * 成功响应（自定义信息+数据）
     * @param msg 响应信息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> success(String msg, T data) {
        return new Result<>(LogisticsConstant.SUCCESS_CODE, msg, data);
    }

    /**
     * 失败响应（系统异常）
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> error() {
        return new Result<>(LogisticsConstant.ERROR_CODE, LogisticsConstant.ERROR_MSG);
    }

    /**
     * 失败响应（自定义信息）
     * @param msg 响应信息
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> error(String msg) {
        return new Result<>(LogisticsConstant.ERROR_CODE, msg);
    }

    /**
     * 参数错误响应
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> paramError() {
        return new Result<>(LogisticsConstant.PARAM_ERROR_CODE, LogisticsConstant.PARAM_ERROR_MSG);
    }

    /**
     * 参数错误响应（自定义信息）
     * @param msg 响应信息
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> paramError(String msg) {
        return new Result<>(LogisticsConstant.PARAM_ERROR_CODE, msg);
    }

    /**
     * 自定义响应
     * @param code 响应码
     * @param msg 响应信息
     * @param data 响应数据
     * @param <T> 数据类型
     * @return 响应结果
     */
    public static <T> Result<T> custom(Integer code, String msg, T data) {
        return new Result<>(code, msg, data);
    }

    /**
     * 构建分页响应数据
     * @param list 数据列表
     * @param total 总条数
     * @param <T> 数据类型
     * @return 分页数据Map
     */
    public static <T> Map<String, Object> buildPageResult(T list, Long total) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", LogisticsConstant.DEFAULT_PAGE_NUM);
        result.put("pageSize", LogisticsConstant.DEFAULT_PAGE_SIZE);
        result.put("totalPages", (total + LogisticsConstant.DEFAULT_PAGE_SIZE - 1) / LogisticsConstant.DEFAULT_PAGE_SIZE);
        return result;
    }

    /**
     * 构建分页响应数据（自定义分页参数）
     * @param list 数据列表
     * @param total 总条数
     * @param pageNum 页码
     * @param pageSize 页大小
     * @param <T> 数据类型
     * @return 分页数据Map
     */
    public static <T> Map<String, Object> buildPageResult(T list, Long total, Integer pageNum, Integer pageSize) {
        Map<String, Object> result = new HashMap<>();
        result.put("list", list);
        result.put("total", total);
        result.put("pageNum", pageNum);
        result.put("pageSize", pageSize);
        result.put("totalPages", (total + pageSize - 1) / pageSize);
        return result;
    }
}
