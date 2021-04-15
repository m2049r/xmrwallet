package com.m2049r.xmrwallet.util;

import android.content.Context;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import lombok.RequiredArgsConstructor;
import timber.log.Timber;

@RequiredArgsConstructor
public class ZipRestore {
    final private Context context;
    final private Uri zipUri;

    private File walletRoot;

    private ZipInputStream zip;

    public boolean restore() throws IOException {
        walletRoot = Helper.getWalletRoot(context);
        String walletName = testArchive();
        if (walletName == null) return false;

        walletName = getUniqueName(walletName);

        if (zip != null)
            throw new IllegalStateException("zip already initialized");
        try {
            zip = new ZipInputStream(context.getContentResolver().openInputStream(zipUri));
            for (ZipEntry entry = zip.getNextEntry(); entry != null; zip.closeEntry(), entry = zip.getNextEntry()) {
                File destination;
                final String name = entry.getName();
                if (name.endsWith(".keys")) {
                    destination = new File(walletRoot, walletName + ".keys");
                } else if (name.endsWith(".address.txt")) {
                    destination = new File(walletRoot, walletName + ".address.txt");
                } else {
                    destination = new File(walletRoot, walletName);
                }
                writeFile(destination);
            }
        } finally {
            if (zip != null) zip.close();
        }
        return true;
    }

    private void writeFile(File destination) throws IOException {
        try (OutputStream os = new FileOutputStream(destination)) {
            byte[] buffer = new byte[8192];
            int length;
            while ((length = zip.read(buffer)) > 0) {
                os.write(buffer, 0, length);
            }
        }
    }

    // test the archive to contain files we expect & return the name of the contained wallet or null
    private String testArchive() {
        String walletName = null;
        boolean keys = false;
        ZipInputStream zipStream = null;
        try {
            zipStream = new ZipInputStream(context.getContentResolver().openInputStream(zipUri));
            for (ZipEntry entry = zipStream.getNextEntry(); entry != null;
                 zipStream.closeEntry(), entry = zipStream.getNextEntry()) {
                if (entry.isDirectory())
                    return null;
                final String name = entry.getName();
                if ((new File(name)).getParentFile() != null)
                    return null;
                if (walletName == null) {
                    if (name.endsWith(".keys")) {
                        walletName = name.substring(0, name.length() - ".keys".length());
                        keys = true; // we have they keys
                    } else if (name.endsWith(".address.txt")) {
                        walletName = name.substring(0, name.length() - ".address.txt".length());
                    } else {
                        walletName = name;
                    }
                } else { // we have a wallet name
                    if (name.endsWith(".keys")) {
                        if (!name.equals(walletName + ".keys")) return null;
                        keys = true; // we have they keys
                    } else if (name.endsWith(".address.txt")) {
                        if (!name.equals(walletName + ".address.txt")) return null;
                    } else if (!name.equals(walletName)) return null;
                }
            }
        } catch (IOException ex) {
            return null;
        } finally {
            try {
                if (zipStream != null) zipStream.close();
            } catch (IOException ex) {
                Timber.w(ex);
            }
        }
        // we need the keys at least
        if (keys) return walletName;
        else return null;
    }

    final static Pattern WALLET_PATTERN = Pattern.compile("^(.+) \\(([0-9]+)\\).keys$");

    private String getUniqueName(String name) {
        if (!(new File(walletRoot, name + ".keys")).exists()) // <name> does not exist => it's ok to use
            return name;

        File[] wallets = walletRoot.listFiles(
                (dir, filename) -> {
                    Matcher m = WALLET_PATTERN.matcher(filename);
                    if (m.find())
                        return m.group(1).equals(name);
                    else return false;
                });
        if (wallets.length == 0) return name + " (1)";
        int maxIndex = 0;
        for (File wallet : wallets) {
            try {
                final Matcher m = WALLET_PATTERN.matcher(wallet.getName());
                m.find();
                final int index = Integer.parseInt(m.group(2));
                if (index > maxIndex) maxIndex = index;
            } catch (NumberFormatException ex) {
                // this cannot happen & we can ignore it if it does
            }
        }
        return name + " (" + (maxIndex + 1) + ")";
    }
}
