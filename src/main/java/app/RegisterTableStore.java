package app;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/*
 * Utility class to handle saving and loading the custom hardware register LUT
 * to and from a local CSV file. This ensures the user's register definitions
 * survive between app restarts.
 */
public final class RegisterTableStore
{
    private static final String DIR_NAME = ".uallintapp";
    private static final String FILE_NAME = "register_lut.csv";

    private RegisterTableStore()
    {
    }

    /*
     * Figures out the default save location for the CSV file.
     * Drops it into a hidden folder in the user's home directory.
     */
    public static Path defaultPath()
    {
        String home = System.getProperty("user.home");
        return Path.of(home, DIR_NAME, FILE_NAME);
    }

    /*
     * Reads the CSV file line by line and converts the data back into
     * RegisterEntry objects for the JavaFX table.
     */
    public static List<RegisterEntry> load(Path path)
    {
        List<RegisterEntry> rows = new ArrayList<>();

        // If the file doesn't exist yet (e.g., first run), just return an empty list
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

                // Skip the CSV header row
                if (trimmed.startsWith("register") == true)
                {
                    continue;
                }

                // Split the line into exactly 2 parts: name and address
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
            // Silently fail and return whatever we managed to read so far
            return rows;
        }

        return rows;
    }

    /*
     * Writes the current list of registers out to the CSV file.
     * Creates the hidden directory first if it doesn't already exist.
     */
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
            // Write the CSV header
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

    /*
     * Cleans up user input by replacing commas with spaces.
     * Since we are saving as a CSV (Comma-Separated Values), a stray comma
     * in a register name would completely corrupt the file structure.
     */
    private static String sanitize(String s)
    {
        if (s == null)
        {
            return "";
        }

        return s.replace(",", " ");
    }
}