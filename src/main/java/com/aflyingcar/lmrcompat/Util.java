package com.aflyingcar.lmrcompat;

import net.blacklab.lmr.util.FileList;

import java.io.File;
import java.io.FileInputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Util {
    private static boolean loadedAlready = false;

    public static void loadResources() {
        if(loadedAlready) return;

        List<File> mod_file = FileList.getModFile("lmrcompat", "lmrcompat");

        if(mod_file.isEmpty()) {
            System.err.println("Uh oh, for some reason we couldn't find ourselves! But if so, then how are we even running?");
            return;
        }

        // I'm just going to assume that we're the _only_ mod called lmrcompat. If we aren't, then, well, shit.
        File self = mod_file.get(0);

        // Copied from LMR's ManagerBase.java:59
        if (self.isFile() && (self.getName().endsWith(".zip") || self.getName().endsWith(".jar"))) {

            // Copied from LMR's ManagerBase.java:112
            try {
                FileInputStream fileinputstream = new FileInputStream(self);
                ZipInputStream zipinputstream = new ZipInputStream(fileinputstream);
                ZipEntry zipentry;

                do {
                    zipentry = zipinputstream.getNextEntry();
                    if(zipentry == null) {
                        break;
                    }
                    if (!zipentry.isDirectory()) {
                        String lname = zipentry.getName();

                        if(lname.contains("assets/")) {
                            // TODO: Load the assets here... somehow
                        }
                    }
                } while(true);

                zipinputstream.close();
                fileinputstream.close();
            }
            catch (Exception exception) {
                System.err.println("Failed to load the zip file.");
            }

        }

        loadedAlready = true;
    }
}
