/*
 * Copyright (c) 2018 m2049r
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.m2049r.xmrwallet.util;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class RestoreHeight {
    static private RestoreHeight Singleton = null;

    static public RestoreHeight getInstance() {
        if (Singleton == null) {
            synchronized (RestoreHeight.class) {
                if (Singleton == null) {
                    Singleton = new RestoreHeight();
                }
            }
        }
        return Singleton;
    }

    private Map<String, Long> blockheight = new HashMap<>();

    RestoreHeight() {
        blockheight.put("2014-01-01", 0L);
        blockheight.put("2014-02-01", 0L);
        blockheight.put("2014-03-01", 0L);
        blockheight.put("2014-04-01", 0L);
        blockheight.put("2014-05-01", 18870L);
        blockheight.put("2014-06-01", 65430L);
        blockheight.put("2014-07-01", 108900L);
        blockheight.put("2014-08-01", 153600L);
        blockheight.put("2014-09-01", 198090L);
        blockheight.put("2014-10-01", 241110L);
        blockheight.put("2014-11-01", 285360L);
        blockheight.put("2014-12-01", 328140L);
        blockheight.put("2015-01-01", 372420L);
        blockheight.put("2015-02-01", 416580L);
        blockheight.put("2015-03-01", 456690L);
        blockheight.put("2015-04-01", 501090L);
        blockheight.put("2015-05-01", 543990L);
        blockheight.put("2015-06-01", 588330L);
        blockheight.put("2015-07-01", 631200L);
        blockheight.put("2015-08-01", 675510L);
        blockheight.put("2015-09-01", 719730L);
        blockheight.put("2015-10-01", 762480L);
        blockheight.put("2015-11-01", 806580L);
        blockheight.put("2015-12-01", 849090L);
        blockheight.put("2016-01-01", 892920L);
        blockheight.put("2016-02-01", 936780L);
        blockheight.put("2016-03-01", 977730L);
        blockheight.put("2016-04-01", 1015860L);
        blockheight.put("2016-05-01", 1037430L);
        blockheight.put("2016-06-01", 1059660L);
        blockheight.put("2016-07-01", 1081290L);
        blockheight.put("2016-08-01", 1103640L);
        blockheight.put("2016-09-01", 1125990L);
        blockheight.put("2016-10-01", 1147620L);
        blockheight.put("2016-11-01", 1169820L);
        blockheight.put("2016-12-01", 1191450L);
        blockheight.put("2017-01-01", 1213920L);
        blockheight.put("2017-02-01", 1236240L);
        blockheight.put("2017-04-01", 1278630L);
        blockheight.put("2017-05-01", 1300260L);
        blockheight.put("2017-06-01", 1322580L);
        blockheight.put("2017-07-01", 1344240L);
        blockheight.put("2017-08-01", 1366680L);
        blockheight.put("2017-09-01", 1389120L);
        blockheight.put("2017-10-01", 1410750L);
        blockheight.put("2017-11-01", 1433070L);
        blockheight.put("2017-12-01", 1454670L);
        blockheight.put("2018-01-01", 1477260L);
        blockheight.put("2018-02-01", 1499640L);
    }

    public long getHeight(String date) {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        parser.setLenient(false);
        try {
            Calendar cal = Calendar.getInstance();
            cal.setTime(parser.parse(date));
            cal.add(Calendar.DAY_OF_MONTH, -2); // account for timezone uncertainty
            if (cal.get(Calendar.YEAR) < 2014) return 0;

            SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
            cal.set(Calendar.DAY_OF_MONTH, 1);
            String lookupDate = formatter.format(cal.getTime());
            Long bc = blockheight.get(lookupDate);
            if (bc == null) {
                while (bc == null) {
                    cal.add(Calendar.MONTH, -1);
                    lookupDate = formatter.format(cal);
                    bc = blockheight.get(lookupDate);
                }
            }
            return bc;
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }

    }
}
