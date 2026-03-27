package org.example.time;

import java.time.YearMonth;

/**
 * @author : hejiale
 * @version : 1.0
 * @date 2025/3/5 20:42
 */
public class TimeUtil {
    public static void main(String[] args) {
        String date = "2025-2";
        YearMonth c = YearMonth.of(2025, 2);
        //YearMonth yearMonth = YearMonth.parse(c);
        System.out.println(c.lengthOfMonth());
    }
}
