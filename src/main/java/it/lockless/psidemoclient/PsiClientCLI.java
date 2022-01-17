package it.lockless.psidemoclient;
import picocli.CommandLine;

@CommandLine.Command(name = "psiClientCLI", mixinStandardHelpOptions = true, version = "psiClientCLI 1.0", description = "Demo implementation of a PSI client using the psi-sdk.")
public class PsiClientCLI implements Runnable{

    public static void main(String... args) {
        int exitCode = new CommandLine(new it.lockless.psidemoclient.PsiClientCLI()).execute(args);
        System.exit(exitCode);
    }

    @Override
    public void run() {
        System.out.println("Hello world");
    }
}
