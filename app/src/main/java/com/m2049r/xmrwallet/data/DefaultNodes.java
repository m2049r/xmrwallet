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

@Getter
@AllArgsConstructor
public enum DefaultNodes {
    BOLDSUCK("xmr-de.boldsuck.org:18080/mainnet/boldsuck.org"),
    boldsuck("6dsdenp6vjkvqzy4wzsnzn6wixkdzihx3khiumyzieauxuxslmcaeiad.onion:18081/mainnet/boldsuck.onion"),
    CAKE("xmr-node.cakewallet.com:18081/mainnet/cakewallet.com"),
    DS_JETZT("monero.ds-jetzt.de:18089/mainnet/ds-jetzt.de"),
    ds_jetzt("qvlr4w7yhnjrdg3txa72jwtpnjn4ezsrivzvocbnvpfbdo342fahhoad.onion:18089/mainnet/ds-jetzt.onion"),
    MONERODEVS("node.monerodevs.org:18089/mainnet/monerodevs.org"),
    MONERUJO("nodex.monerujo.io:18081/mainnet/monerujo.io"),
    monerujo("monerujods7mbghwe6cobdr6ujih6c22zu5rl7zshmizz2udf7v7fsad.onion:18081/mainnet/monerujo.onion"),
    SETH("node.sethforprivacy.com:18089/mainnet/sethforprivacy.com"),
    seth("sfpp2p7wnfjv3lrvfan4jmmkvhnbsbimpa3cqyuf7nt6zd24xhcqcsyd.onion/mainnet/sethforprivacy.onion"),
    STACK("monero.stackwallet.com:18081/mainnet/stackwallet.com"),
    XMRROCKS("node.xmr.rocks:18089/mainnet/xmr.rocks"),
    xmrrocks("xqnnz2xmlmtpy2p4cm4cphg2elkwu5oob7b7so5v4wwgt44p6vbx5ryd.onion/mainnet/xmr.rocks.onion"),
    XMRTW("opennode.xmr-tw.org:18089/mainnet/xmr-tw.org");

    private final String uri;
}
