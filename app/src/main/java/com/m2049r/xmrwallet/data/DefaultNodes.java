/*
 * Copyright (c) 2020 m2049r
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

package com.m2049r.xmrwallet.data;

import lombok.AllArgsConstructor;
import lombok.Getter;

// Nodes stolen from https://moneroworld.com/#nodes

@AllArgsConstructor
public enum DefaultNodes {
    MONERUJO("nodex.monerujo.io:18081"),
    XMRTO("node.xmr.to:18081"),
    SUPPORTXMR("node.supportxmr.com:18081"),
    HASHVAULT("nodes.hashvault.pro:18081"),
    MONEROWORLD("node.moneroworld.com:18089"),
    XMRTW("opennode.xmr-tw.org:18089"),
    ds_jetzt("monero.ds-jetzt.de:18089"),
    MONERUJO_ONION("monerujods7mbghwe6cobdr6ujih6c22zu5rl7zshmizz2udf7v7fsad.onion:18081/mainnet/monerujo.onion"),
    Criminales78("56wl7y2ebhamkkiza4b7il4mrzwtyvpdym7bm2bkg3jrei2je646k3qd.onion:18089/mainnet/Criminales78.onion"),
    xmrfail("mxcd4577fldb3ppzy7obmmhnu3tf57gbcbd4qhwr2kxyjj2qi3dnbfqd.onion:18081/mainnet/xmrfail.onion"),
    boldsuck("6dsdenp6vjkvqzy4wzsnzn6wixkdzihx3khiumyzieauxuxslmcaeiad.onion:18081/mainnet/boldsuck.onion"),
    ds_jetzt_onion("qvlr4w7yhnjrdg3txa72jwtpnjn4ezsrivzvocbnvpfbdo342fahhoad.onion:18089/mainnet/ds-jetzt.onion");

    @Getter
    private final String uri;
}
