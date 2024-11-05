package com.m2049r.xmrwallet.service.shift;

import androidx.annotation.NonNull;

import com.m2049r.xmrwallet.R;
import com.m2049r.xmrwallet.data.Crypto;
import com.m2049r.xmrwallet.fragment.send.PreShifter;
import com.m2049r.xmrwallet.fragment.send.Shifter;
import com.m2049r.xmrwallet.service.shift.api.ShiftApi;
import com.m2049r.xmrwallet.service.shift.process.PreProcess;
import com.m2049r.xmrwallet.service.shift.process.PreShiftProcess;
import com.m2049r.xmrwallet.service.shift.process.Process;
import com.m2049r.xmrwallet.service.shift.process.ShiftProcess;
import com.m2049r.xmrwallet.service.shift.provider.exolix.ExolixApiImpl;

import java.util.HashSet;
import java.util.Set;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@RequiredArgsConstructor
@Getter
public enum ShiftService {
    XMRTO(false, "xmr.to", "xmrto", null, null, 0, R.drawable.ic_xmrto_logo, ""),
    SIDESHIFT(false, "SideShift.ai", "side", null, null, R.drawable.ic_sideshift_icon, R.drawable.ic_sideshift_wide, ""),
    EXOLIX(true, "EXOLIX", "exolix", new ExolixApiImpl(), Type.ONESTEP, R.drawable.ic_exolix_icon, R.drawable.ic_exolix_wide, "XMR:BTC:LTC:ETH:USDT:SOL"),
    UNKNOWN(false, "", null, null, null, 0, 0, "");

    static final public ShiftService DEFAULT = EXOLIX;
    final private boolean enabled;
    final private String label;
    final private String tag;
    final private ShiftApi shiftApi;
    final private Type type;
    final private int iconId;
    final private int logoId;
    final private String assets;

    @NonNull
    static public ShiftService findWithTag(String tag) {
        if (tag == null) return UNKNOWN;
        for (ShiftService service : values()) {
            if (tag.equals(service.tag)) return service;
        }
        return UNKNOWN;
    }

    @Getter
    static private final Set<Crypto> possibleAssets = new HashSet<>();

    static {
        assert DEFAULT.enabled;
        for (ShiftService service : values()) {
            if (!service.enabled) continue;
            final String[] assets = service.getAssets().split(":");
            for (String anAsset : assets) {
                possibleAssets.add(Crypto.withSymbol(anAsset));
            }
        }
    }

    public static boolean isAssetSupported(@NonNull Crypto crypto) {
        return possibleAssets.contains(crypto);
    }

    public static boolean isAssetSupported(@NonNull String symbol) {
        final Crypto crypto = Crypto.withSymbol(symbol);
        if (crypto != null) {
            return isAssetSupported(crypto);
        }
        return false;
    }

    public boolean supportsAsset(@NonNull Crypto crypto) {
        return assets.contains(crypto.getSymbol());
    }

    public boolean supportsAsset(@NonNull String symbol) {
        final Crypto crypto = Crypto.withSymbol(symbol);
        if (crypto != null) {
            return supportsAsset(crypto);
        }
        return false;
    }

    public ShiftProcess createProcess(Shifter shifter) {
        return new Process(this, shifter);
    }

    public PreShiftProcess createPreProcess(PreShifter shifter) {
        return new PreProcess(this, shifter);
    }

    public enum Type {
        ONESTEP, TWOSTEP;
    }

    public static Crypto ASSET = null; // keep asset to exchange globally
}