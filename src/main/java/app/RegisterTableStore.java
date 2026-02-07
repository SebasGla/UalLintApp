package app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public final class RegisterTableStore
{
    private static final String DIR_NAME = ".uallintapp";
    private static final String FILE_NAME = "register_lut.csv";

    private RegisterTableStore()
    {
    }

    public static Path defaultPath()
    {
        String home = System.getProperty("user.home");
        return Path.of(home, DIR_NAME, FILE_NAME);
    }

    public static List<RegisterEntry> load(Path path)
    {
        List<RegisterEntry> rows = new ArrayList<>();

        if (Files.exists(path) == false)
        {
            return rows;
        }

        try (BufferedReader br = Files.newBufferedReader(path, StandardCharsets.UTF_8))
        {
            String line;

            while ((line = br.readLine()) != null)
            {
                String trimmed = line.trim();

                if (trimmed.isEmpty() == true)
                {
                    continue;
                }

                if (trimmed.startsWith("register") == true)
                {
                    continue;
                }

                String[] parts = trimmed.split(",", 2);

                if (parts.length < 2)
                {
                    continue;
                }

                rows.add(new RegisterEntry(parts[0].trim(), parts[1].trim()));
            }
        }
        catch (Exception ignored)
        {
            return rows;
        }

        return rows;
    }

    public static void save(Path path, List<RegisterEntry> rows)
    {
        try
        {
            Files.createDirectories(path.getParent());
        }
        catch (Exception ignored)
        {
            return;
        }

        try (BufferedWriter bw = Files.newBufferedWriter(path, StandardCharsets.UTF_8))
        {
            bw.write("registerName,address");
            bw.newLine();

            for (RegisterEntry r : rows)
            {
                bw.write(sanitize(r.getRegisterName()));
                bw.write(",");
                bw.write(sanitize(r.getAddress()));
                bw.newLine();
            }
        }
        catch (Exception ignored)
        {
        }
    }

    private static String sanitize(String s)
    {
        if (s == null)
        {
            return "";
        }

        return s.replace(",", " ");
    }
}
