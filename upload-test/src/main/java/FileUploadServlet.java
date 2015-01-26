import com.elopteryx.paint.upload.OnError;
import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.OnRequestComplete;
import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.UploadParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.List;

@WebServlet(value = "/FileUploadServlet", asyncSupported = true)
public class FileUploadServlet extends HttpServlet {

    /**
     * Directory where uploaded files will be saved, its relative to
     * the web application directory.
     */
    private static final String UPLOAD_DIR = "uploads";

    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
            throws ServletException, IOException {


        // gets absolute path of the web application
        String applicationPath = request.getServletContext().getRealPath("");
        // constructs path of the directory to save uploaded file
        final Path uploadFilePath = Paths.get(applicationPath, UPLOAD_DIR);

        if (!Files.isDirectory(uploadFilePath)) {
            Files.createDirectories(uploadFilePath);
        }
        System.out.println("Upload File Directory=" + uploadFilePath.toAbsolutePath().toString());

        // Check that we have a file upload request
        if (!UploadParser.isMultipart(request))
            throw new ServletException("Not multipart!");

        final List<ByteArrayOutputStream> formFields = new ArrayList<>();

        final StringBuilder builder = new StringBuilder();

        UploadParser.newParser(request, response)
                .onPartBegin(new OnPartBegin() {
                    @Override
                    public WritableByteChannel apply(UploadContext context, ByteBuffer buffer) throws IOException {
                        System.out.println("Start!");
                        //use the buffer to detect file type
                        PartStream part = context.getCurrentPart();
                        String name = part.getName();
                        if (part.isFile()) {
                            if ("".equals(part.getSubmittedFileName()))
                                throw new IOException("No file was chosen for the form field!");
                            System.out.println("File field " + name + " with file name "
                                    + part.getSubmittedFileName() + " detected!");
                            for(String header : part.getHeaderNames())
                                System.out.println(header + " " + part.getHeader(header));
                            part.getHeaders("content-type");
                            System.out.println(part.getContentType());
                            builder.append(part.getSubmittedFileName() + ",");
                            Path path = uploadFilePath.resolve(part.getSubmittedFileName());
                            return Channels.newChannel(Files.newOutputStream(path));
                        } else {
                            for(String header : part.getHeaderNames())
                                System.out.println(header + " " + part.getHeader(header));
                            System.out.println(part.getContentType());
                            ByteArrayOutputStream baos = new ByteArrayOutputStream();
                            formFields.add(baos);
                            return Channels.newChannel(baos);
                        }
                    }
                })
                .onPartEnd(new OnPartEnd() {
                    @Override
                    public void accept(UploadContext context, WritableByteChannel channel) throws IOException {
                        channel.close();
                        System.out.println(context.getCurrentPart().getKnownSize());
                        System.out.println("Part success!");
                    }
                })
                .onComplete(new OnRequestComplete() {
                    @Override
                    public void accept(UploadContext context) throws IOException, ServletException {
                        System.out.println("Success!");
                        request.setAttribute("message", builder + " File uploaded successfully!");
                        getServletContext().getRequestDispatcher("/response.jsp").forward(request, response);
                        for (ByteArrayOutputStream baos : formFields)
                            System.out.println(baos.toString());
                        context.getResponse().setStatus(200);
                    }
                })
                .onError(new OnError() {
                    @Override
                    public void accept(UploadContext context, Throwable throwable) {
                        System.out.println("Error!");
                        throwable.printStackTrace();
                        for (ByteArrayOutputStream baos : formFields)
                            System.out.println(baos.toString());
                        try {
                            context.getResponse().sendError(500);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                })
                .sizeThreshold(4096)
                .maxPartSize(Long.MAX_VALUE)
                .maxRequestSize(Long.MAX_VALUE)
                .setup();
    }

}
