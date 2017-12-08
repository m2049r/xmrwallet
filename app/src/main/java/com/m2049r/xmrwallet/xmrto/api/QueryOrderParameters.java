/*
 * Copyright (c) 2017 m2049r et al.
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

package com.m2049r.xmrwallet.xmrto.api;

public interface QueryOrderParameters {

    double getLowerLimit(); //  "lower_limit": <lower_order_limit_in_btc_as_float>,

    double getPrice(); // "price": <price_of_1_btc_in_xmr_as_offered_by_service_as_float>,

    double getUpperLimit(); // "upper_limit": <upper_order_limit_in_btc_as_float>,

    boolean isZeroConfEnabled(); // "zero_conf_enabled": <true_if_zero_conf_is_enabled_as_boolean>,

    double getZeroConfMaxAmount(); // "zero_conf_max_amount": <up_to_this_amount_zero_conf_is_possible_as_float>

}
