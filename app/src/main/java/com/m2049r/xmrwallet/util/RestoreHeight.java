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
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;

public class RestoreHeight {
    static final int DIFFICULTY_TARGET = 120; // seconds

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
        blockheight.put("2014-05-01", 18844L);
        blockheight.put("2014-06-01", 65406L);
        blockheight.put("2014-07-01", 108882L);
        blockheight.put("2014-08-01", 153594L);
        blockheight.put("2014-09-01", 198072L);
        blockheight.put("2014-10-01", 241088L);
        blockheight.put("2014-11-01", 285305L);
        blockheight.put("2014-12-01", 328069L);
        blockheight.put("2015-01-01", 372369L);
        blockheight.put("2015-02-01", 416505L);
        blockheight.put("2015-03-01", 456631L);
        blockheight.put("2015-04-01", 501084L);
        blockheight.put("2015-05-01", 543973L);
        blockheight.put("2015-06-01", 588326L);
        blockheight.put("2015-07-01", 631187L);
        blockheight.put("2015-08-01", 675484L);
        blockheight.put("2015-09-01", 719725L);
        blockheight.put("2015-10-01", 762463L);
        blockheight.put("2015-11-01", 806528L);
        blockheight.put("2015-12-01", 849041L);
        blockheight.put("2016-01-01", 892866L);
        blockheight.put("2016-02-01", 936736L);
        blockheight.put("2016-03-01", 977691L);
        blockheight.put("2016-04-01", 1015848L);
        blockheight.put("2016-05-01", 1037417L);
        blockheight.put("2016-06-01", 1059651L);
        blockheight.put("2016-07-01", 1081269L);
        blockheight.put("2016-08-01", 1103630L);
        blockheight.put("2016-09-01", 1125983L);
        blockheight.put("2016-10-01", 1147617L);
        blockheight.put("2016-11-01", 1169779L);
        blockheight.put("2016-12-01", 1191402L);
        blockheight.put("2017-01-01", 1213861L);
        blockheight.put("2017-02-01", 1236197L);
        blockheight.put("2017-03-01", 1256358L);
        blockheight.put("2017-04-01", 1278622L);
        blockheight.put("2017-05-01", 1300239L);
        blockheight.put("2017-06-01", 1322564L);
        blockheight.put("2017-07-01", 1344225L);
        blockheight.put("2017-08-01", 1366664L);
        blockheight.put("2017-09-01", 1389113L);
        blockheight.put("2017-10-01", 1410738L);
        blockheight.put("2017-11-01", 1433039L);
        blockheight.put("2017-12-01", 1454639L);
        blockheight.put("2018-01-01", 1477201L);
        blockheight.put("2018-02-01", 1499599L);
        blockheight.put("2018-03-01", 1519796L);
        blockheight.put("2018-04-01", 1542067L);
        blockheight.put("2018-05-01", 1562861L);
        blockheight.put("2018-06-01", 1585135L);
        blockheight.put("2018-07-01", 1606715L);
        blockheight.put("2018-08-01", 1629017L);
        blockheight.put("2018-09-01", 1651347L);
        blockheight.put("2018-10-01", 1673031L);
        blockheight.put("2018-11-01", 1695128L);
        blockheight.put("2018-12-01", 1716687L);
        blockheight.put("2019-01-01", 1738923L);
        blockheight.put("2019-02-01", 1761435L);
        blockheight.put("2019-03-01", 1781681L);
        blockheight.put("2019-04-01", 1803081L);
        blockheight.put("2019-05-01", 1824671L);
        blockheight.put("2019-06-01", 1847005L);
        blockheight.put("2019-07-01", 1868590L);
        blockheight.put("2019-08-01", 1890878L);
        blockheight.put("2019-09-01", 1913201L);
        blockheight.put("2019-10-01", 1934732L);
        blockheight.put("2019-11-01", 1957051L);
        blockheight.put("2019-12-01", 1978433L);
        blockheight.put("2020-01-01", 2001315L);
        blockheight.put("2020-02-01", 2023656L);
        blockheight.put("2020-03-01", 2044552L);
        blockheight.put("2020-04-01", 2066806L);
        blockheight.put("2020-05-01", 2088411L);
        blockheight.put("2020-06-01", 2110702L);
        blockheight.put("2020-07-01", 2132318L);
        blockheight.put("2020-08-01", 2154590L);
        blockheight.put("2020-09-01", 2176790L);
        blockheight.put("2020-10-01", 2198370L);
        blockheight.put("2020-11-01", 2220670L);
        blockheight.put("2020-12-01", 2242241L);
        blockheight.put("2021-01-01", 2264584L);
        blockheight.put("2021-02-01", 2286892L);
        blockheight.put("2021-03-01", 2307079L);
        blockheight.put("2021-04-01", 2329385L);
        blockheight.put("2021-05-01", 2351004L);
        blockheight.put("2021-06-01", 2373306L);
        blockheight.put("2021-07-01", 2394882L);
        blockheight.put("2021-08-01", 2417162L);
        blockheight.put("2021-09-01", 2439490L);
        blockheight.put("2021-10-01", 2461020L);
        blockheight.put("2021-11-01", 2483377L);
        blockheight.put("2021-12-01", 2504932L);
        blockheight.put("2022-01-01", 2527316L);
        blockheight.put("2022-02-01", 2549605L);
        blockheight.put("2022-03-01", 2569711L);
        blockheight.put("2022-04-01", 2591995L);
        blockheight.put("2022-05-01", 2613603L);
        blockheight.put("2022-06-01", 2635840L);
        blockheight.put("2022-07-01", 2657395L);
        blockheight.put("2022-08-01", 2679705L);
        blockheight.put("2022-09-01", 2701991L);
        blockheight.put("2022-10-01", 2723607L);
        blockheight.put("2022-11-01", 2745899L);
        blockheight.put("2022-12-01", 2767427L);
        blockheight.put("2023-01-01", 2789763L);
        blockheight.put("2023-02-01", 2811996L);
        blockheight.put("2023-03-01", 2832118L);
        blockheight.put("2023-04-01", 2854365L);
        blockheight.put("2023-05-01", 2875972L);
    }

    public long getHeight(String date) {
        SimpleDateFormat parser = new SimpleDateFormat("yyyy-MM-dd");
        parser.setTimeZone(TimeZone.getTimeZone("UTC"));
        parser.setLenient(false);
        try {
            return getHeight(parser.parse(date));
        } catch (ParseException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public long getHeight(final Date date) {
        Calendar cal = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
        cal.set(Calendar.DST_OFFSET, 0);
        cal.setTime(date);
        cal.add(Calendar.DAY_OF_MONTH, -4); // give it some leeway
        if (cal.get(Calendar.YEAR) < 2014)
            return 0;
        if ((cal.get(Calendar.YEAR) == 2014) && (cal.get(Calendar.MONTH) <= 3))
            // before May 2014
            return 0;

        Calendar query = (Calendar) cal.clone();

        SimpleDateFormat formatter = new SimpleDateFormat("yyyy-MM-dd");
        formatter.setTimeZone(TimeZone.getTimeZone("UTC"));

        String queryDate = formatter.format(date);

        cal.set(Calendar.DAY_OF_MONTH, 1);
        long prevTime = cal.getTimeInMillis();
        String prevDate = formatter.format(prevTime);
        // lookup blockheight at first of the month
        Long prevBc = blockheight.get(prevDate);
        if (prevBc == null) {
            // if too recent, go back in time and find latest one we have
            while (prevBc == null) {
                cal.add(Calendar.MONTH, -1);
                if (cal.get(Calendar.YEAR) < 2014) {
                    throw new IllegalStateException("endless loop looking for blockheight");
                }
                prevTime = cal.getTimeInMillis();
                prevDate = formatter.format(prevTime);
                prevBc = blockheight.get(prevDate);
            }
        }
        long height = prevBc;
        // now we have a blockheight & a date ON or BEFORE the restore date requested
        if (queryDate.equals(prevDate)) return height;
        // see if we have a blockheight after this date
        cal.add(Calendar.MONTH, 1);
        long nextTime = cal.getTimeInMillis();
        String nextDate = formatter.format(nextTime);
        Long nextBc = blockheight.get(nextDate);
        if (nextBc != null) { // we have a range - interpolate the blockheight we are looking for
            long diff = nextBc - prevBc;
            long diffDays = TimeUnit.DAYS.convert(nextTime - prevTime, TimeUnit.MILLISECONDS);
            long days = TimeUnit.DAYS.convert(query.getTimeInMillis() - prevTime,
                    TimeUnit.MILLISECONDS);
            height = Math.round(prevBc + diff * (1.0 * days / diffDays));
        } else {
            long days = TimeUnit.DAYS.convert(query.getTimeInMillis() - prevTime,
                    TimeUnit.MILLISECONDS);
            height = Math.round(prevBc + 1.0 * days * (24f * 60 * 60 / DIFFICULTY_TARGET));
        }
        return height;
    }
}
