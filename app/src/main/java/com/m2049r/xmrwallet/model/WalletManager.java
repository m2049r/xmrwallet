package com.m2049r.xmrwallet.model;

import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class WalletManager {
    final static String TAG = "WalletManager";

    static {
        System.loadLibrary("monerujo");
    }

    // no need to keep a reference to the REAL WalletManager (we get it every tvTime we need it)
    private static WalletManager Instance = null;

    public static WalletManager getInstance() { // TODO not threadsafe
        if (WalletManager.Instance == null) {
            WalletManager.Instance = new WalletManager();
        }
        return WalletManager.Instance;
    }

    public Wallet createWallet(String path, String password, String language, boolean isTestNet) {
        long walletHandle = createWalletJ(path, password, language, isTestNet);
        return new Wallet(walletHandle);
    }

    public Wallet openWallet(String path, String password, boolean isTestNet) {
        long walletHandle = openWalletJ(path, password, isTestNet);
        return new Wallet(walletHandle);
    }

    public Wallet recoveryWallet(String path, String mnemonic, boolean isTestNet) {
        return recoveryWallet(path, mnemonic, isTestNet, 0);
    }

    public Wallet recoveryWallet(String path, String mnemonic, boolean isTestNet, long restoreHeight) {
        long walletHandle = recoveryWalletJ(path, mnemonic, isTestNet, restoreHeight);
        return new Wallet(walletHandle);
    }

    private native long createWalletJ(String path, String password, String language, boolean isTestNet);

    private native long openWalletJ(String path, String password, boolean isTestNet);

    private native long recoveryWalletJ(String path, String mnemonic, boolean isTestNet, long restoreHeight);

    private native long createWalletFromKeysJ(String path, String language,
                                              boolean isTestNet,
                                              long restoreHeight,
                                              String addressString,
                                              String viewKeyString,
                                              String spendKeyString);

    public native boolean walletExists(String path);

    public native boolean verifyWalletPassword(String keys_file_name, String password, boolean watch_only);

    //public native List<String> findWallets(String path); // this does not work - some error in boost

    public class WalletInfo {
        public File path;
        public String name;
        public String address;
    }

    public List<WalletInfo> findWallets(File path) {
        List<WalletInfo> wallets = new ArrayList<>();
        Log.d(TAG, "Scanning: " + path.getAbsolutePath());
        File[] found = path.listFiles(new FilenameFilter() {
            public boolean accept(File dir, String filename) {
                return filename.endsWith(".keys");
            }
        });
        for (int i = 0; i < found.length; i++) {
            WalletInfo info = new WalletInfo();
            info.path = path;
            String filename = found[i].getName();
            info.name = filename.substring(0, filename.length() - 5); // 5 is length of ".keys"+1
            File addressFile = new File(path, info.name + ".address.txt");
            Log.d(TAG, addressFile.getAbsolutePath());
            info.address = "??????";
            BufferedReader addressReader = null;
            try {
                addressReader = new BufferedReader(new FileReader(addressFile));
                info.address = addressReader.readLine();
            } catch (IOException ex) {
                Log.d(TAG, ex.getLocalizedMessage());
            } finally {
                if (addressReader != null) {
                    try {
                        addressReader.close();
                    } catch (IOException ex) {
                        // that's just too bad
                    }
                }
            }
            wallets.add(info);
        }
        return wallets;
    }

    public native String getErrorString();

//TODO virtual bool checkPayment(const std::string &address, const std::string &txid, const std::string &txkey, const std::string &daemon_address, uint64_t &received, uint64_t &height, std::string &error) const = 0;

    String daemonAddress = null;

    public void setDaemonAddress(String address) {
        this.daemonAddress = address;
        setDaemonAddressJ(address);
    }

    public String getDaemonAddress() {
        return this.daemonAddress;
    }

    private native void setDaemonAddressJ(String address);

    public native int getConnectedDaemonVersion();

    public native long getBlockchainHeight();

    public native long getBlockchainTargetHeight();

    public native long getNetworkDifficulty();

    public native double getMiningHashRate();

    public native long getBlockTarget();

    public native boolean isMining();

    public native boolean startMining(String address, boolean background_mining, boolean ignore_battery);

    public native boolean stopMining();

    public native String resolveOpenAlias(String address, boolean dnssec_valid);

//TODO static std::tuple<bool, std::string, std::string, std::string, std::string> checkUpdates(const std::string &software, const std::string &subdir);


}