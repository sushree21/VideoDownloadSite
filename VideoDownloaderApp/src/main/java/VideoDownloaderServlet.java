import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;

@WebServlet("/download")
public class VideoDownloaderServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String videoUrl = request.getParameter("videoUrl");
        String fileName = request.getParameter("fileName");

        if (videoUrl == null || fileName == null || videoUrl.isEmpty() || fileName.isEmpty()) {
            response.getWriter().write("Invalid input! Please provide a video URL and a filename.");
            return;
        }

        // Temporary directory for saving the video on the server
        String tempFolder = System.getProperty("java.io.tmpdir") + "videos";

        // Ensure the directory exists
        File folder = new File(tempFolder);
        if (!folder.exists() && !folder.mkdirs()) {
            response.getWriter().write("Failed to create a temporary directory for video download.");
            return;
        }

        // Define the full path of the temporary video file
        String tempFilePath = tempFolder + File.separator + fileName + ".mp4";

        try {
            // Download the video to the temporary folder
            downloadVideo(videoUrl, tempFolder, fileName);

            // Serve the file to the client as a downloadable attachment
            File tempFile = new File(tempFilePath);
            if (tempFile.exists()) {
                response.setContentType("video/mp4");
                response.setHeader("Content-Disposition", "attachment; filename=\"" + fileName + ".mp4\"");
                response.setContentLengthLong(tempFile.length());

                try (FileInputStream fileInputStream = new FileInputStream(tempFile);
                     var outputStream = response.getOutputStream()) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = fileInputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                }

                // Clean up the temporary file after serving
                if (!tempFile.delete()) {
                    System.err.println("Failed to delete temporary file: " + tempFilePath);
                }
            } else {
                response.getWriter().write("Error: File not found on the server.");
            }
        } catch (Exception e) {
            response.getWriter().write("Failed to download and serve video: " + e.getMessage());
        }
    }

    private void downloadVideo(String url, String saveFolder, String fileName) throws IOException, InterruptedException {
        String ffmpegPath = "C:/ffmpeg-2024-09-19-git-0d5b68c27c-full_build/bin/ffmpeg.exe";
        String outputTemplate = saveFolder + File.separator + fileName + ".mp4";

        ProcessBuilder pb = new ProcessBuilder(
            "yt-dlp",
            "-o", outputTemplate,
            "-f", "bestvideo+bestaudio/best",
            "--merge-output-format", "mp4",
            "--ffmpeg-location", ffmpegPath,
            "--postprocessor-args", "-c:v copy -c:a aac",  // Re-encode audio to AAC
            "-k",
            url
        );

        pb.redirectErrorStream(true);

        Process process = pb.start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                System.out.println(line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            throw new RuntimeException("Error during video download. Exit code: " + exitCode);
        }
        else {
            System.out.println("Download finished successfully.");
        }
    }
}
