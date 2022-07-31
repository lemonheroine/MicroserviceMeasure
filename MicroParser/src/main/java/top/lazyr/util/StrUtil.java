package top.lazyr.util;

import top.lazyr.validator.VarValidator;

import java.util.ArrayList;
import java.util.List;

/**
 * @author lazyr
 * @created 2022/5/15
 */
public class StrUtil {
    private static String SPILT = ",";

    /**
     * List: ["value1", "value2", "value3"]  => String: "value1,value2,value3"
     * @param values
     * @return
     */
    public static String list2Csv(List<String> values) {
        if (VarValidator.empty(values)) {
            return "";
        }
        StringBuilder csv = new StringBuilder();
        for (int i = 0; i < values.size(); i++) {
            csv.append(values.get(i));
            if (i != values.size() - 1) {
                csv.append(SPILT);
            }
        }
        return csv.toString();
    }

    /**
     * String: "value1,value2,value3"  =>  List: ["value1", "value2", "value3"]
     * 若scv为""，则返回size=0的List
     * @param csv
     * @return
     */
    public static List<String> csv2List(String csv) {
        List<String> values = new ArrayList<>();
        String[] valueArr = csv.split(SPILT);
        if (VarValidator.empty(valueArr)) {
            return values;
        }
        for (String value : valueArr) {
            values.add(value);
        }
        return values;
    }
}
