// @formatter:off
package eden.astalpcmx;

import eden.astalpcmx.audio.AstalFormat;
import eden.astalpcmx.audio.AstalOutputSource;
import eden.common.audio.OutputMixer;
import eden.common.audio.OutputSource;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;

import static eden.astalpcmx.ApplicationInformation.*;


/**
 *  This class serves as the entry point to this application. It contains the
 *  main method from which the application initializes.
 *
 *  @author     Brendon
 *  @version    u0r0, 01/17/2019.
 */
public class Main {

    private static void sayHelp() {
        System.out.println(
            "java -jar astalpcmx.jar [loop] <file path>\n"
        );
    }

    /**
     *  The main method is the entry point to this application
     *
     *  @param      args
     *              Command-line arguments to be passed on execution
     */
    public static void main(String[] args) {
        System.out.print("\n\n" +
            APPLICATION_NAME + "\n" +
            "----------\n" +
            APPLICATION_VERSION + " by Brendon, " + APPLICATION_DATE + ".\n\n"
        );
        boolean loop = false;

        if (args.length == 0) {
            System.out.println("<X> No arguments\n");
            sayHelp();
            return;
        }
        for (String s : args) {
            switch (s.toLowerCase()) {
                case "help":
                    sayHelp();
                    return;
                case "loop":
                    loop = true;
            }
        }
        try {
            System.out.print("<i> LOAD...");
            File file = new File(args[args.length - 1]);

            InputStream stream = new BufferedInputStream(
                new FileInputStream(file)
            );
            OutputSource source = new AstalOutputSource(
                stream, AstalFormat.P16, loop
            );
            OutputMixer mixer = new OutputMixer(
                (byte) 1, AstalFormat.P16, OutputMixer.DEFAULT_BUFFER_SIZE * 2
            );
            mixer.attach(source);
            mixer.setHold(false);
            System.out.println("OK\n");

            System.out.printf(
"   Name :  " + file.getName() + "\n" +
"   Size :  " + stream.available() + " bytes" + "\n" +
" Length :  %.2f" + " seconds\n\n", (double) stream.available() / 176400
            );
            new Thread(mixer).start();

            if (loop) {
                System.out.println("<!> Loop enabled, [ctrl+c] to kill.\n");
            } else {
                System.out.println(
                    "<!> Allow some latency after the stream before the " +
                    "program ends.\n"
                );
            }
            System.out.print("<i> PLAYING STREAM...");

            if (!loop) {
                while (!source.isDone()) {
                    Thread.sleep(1000);
                }
                System.out.println("END\n");
                System.exit(0);
            }
        } catch (FileNotFoundException e) {
            System.err.println("<X> File not found");
            System.exit(2);
        } catch (IOException e) {
            System.err.println("<X> I/O error when reading file");
            System.exit(3);
        } catch (InterruptedException e) {
            System.err.println("<X> Main thread unexpectedly interrupted");
            System.exit(4);
        }
    }
}
