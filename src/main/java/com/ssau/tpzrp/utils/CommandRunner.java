package com.ssau.tpzrp.utils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class CommandRunner {
    public static void run(String command) throws IOException {
        runWithReturn(command);
    }
    public static List<String> runWithReturn(String command) throws IOException {
        ProcessBuilder builder = new ProcessBuilder(
                "cmd.exe", "/c", command);
        builder.redirectErrorStream(true);
        Process process = builder.start();

        InputStreamReader inputStreamReader = new InputStreamReader(process.getInputStream());
        BufferedReader r = new BufferedReader(inputStreamReader);

        List<String> fullConsoleOutput = new ArrayList<>();
        String line;

        while (true) {
            line = r.readLine();
            if (Objects.isNull(line)) {
                break;
            }
            fullConsoleOutput.add(line);
            System.out.println(line);
        }
        return fullConsoleOutput;
    }
}
