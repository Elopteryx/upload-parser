import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

@WebServlet(value = "/FileUploadServlet", asyncSupported = true)
public class FileUploadServlet extends HttpServlet {

    /**
     * Directory where uploaded files will be saved, its relative to
     * the web application directory.
     */
    private static final String UPLOAD_DIR = "uploads";

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {


        // gets absolute path of the web application
        String applicationPath = request.getServletContext().getRealPath("");
        // constructs path of the directory to save uploaded file
        Path uploadFilePath = Paths.get(applicationPath, UPLOAD_DIR);

        if (!Files.isDirectory(uploadFilePath)) {
            Files.createDirectories(uploadFilePath);
        }
        System.out.println("Upload File Directory=" + uploadFilePath.toAbsolutePath().toString());

        // Check that we have a file upload request
        if (!UploadParser.isMultipart(request))
            throw new ServletException("Not multipart!");

        StringJoiner joiner = new StringJoiner(",");

        List<ByteArrayOutputStream> formFields = new ArrayList<>();

        UploadParser.newParser(request, response)
                .onPartStart((context, buffer) -> {
                    System.out.println("Start!");
                    //use the buffer to detect file type
                    PartStream part = context.getCurrentPart();
                    System.out.println(part.getSize());
                    try {
                        String name = part.getName();
                        if (part.isFile()) {
                            System.out.println("File field " + name + " with file name "
                                    + part.getSubmittedFileName() + " detected!");
                            part.getHeaderNames().forEach(header -> System.out.println(header + " " + part.getHeader(header)));
                            part.getHeaders("content-type");
                            System.out.println(part.getContentType());
                            joiner.add(part.getSubmittedFileName());
                            Path path = uploadFilePath.resolve(part.getSubmittedFileName());
                            return Channels.newChannel(Files.newOutputStream(path));
                        } else {
                            part.getHeaderNames().forEach(header -> System.out.println(header + " " + part.getHeader(header)));
                            System.out.println(part.getContentType());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            formFields.add(baos);
                            return Channels.newChannel(baos);
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                })
                .onPartFinish((context, output) -> {
                    try {
                        output.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    System.out.println(context.getCurrentPart().getSize());
                    System.out.println("Part success!");
                })
                .onComplete(context -> {
                    System.out.println("Success!");
                    request.setAttribute("message", joiner + " File uploaded successfully!");
                    try {
                        getServletContext().getRequestDispatcher("/response.jsp").forward(request, response);
                    } catch (ServletException | IOException e) {
                        e.printStackTrace();
                    }
                    formFields.stream()
                            .map(ByteArrayOutputStream::toString)
                            .forEach(System.out::println);
                    context.getResponse().setStatus(200);
                })
                .onError((context, t) -> {
                    System.out.println("Error!");
                    t.printStackTrace();
                    formFields.stream()
                            .map(ByteArrayOutputStream::toString)
                            .forEach(System.out::println);
                    try {
                        context.getResponse().sendError(500);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                })
                .sizeThreshold(4096)
                .maxPartSize(Long.MAX_VALUE)
                .maxRequestSize(Long.MAX_VALUE)
                .setup();
    }

}
