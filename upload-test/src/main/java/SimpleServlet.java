import com.elopteryx.paint.upload.OnPartBegin;
import com.elopteryx.paint.upload.OnPartEnd;
import com.elopteryx.paint.upload.PartStream;
import com.elopteryx.paint.upload.UploadContext;
import com.elopteryx.paint.upload.UploadParser;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@WebServlet(value = "/SimpleUploadServlet", asyncSupported = true)
public class SimpleServlet extends HttpServlet {

    /**
     * Directory where uploaded files will be saved, its relative to
     * the web application directory.
     */
    private static final String UPLOAD_DIR = "uploads";

    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {

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

        UploadParser.newParser(request, response)
                .onPartBegin(new OnPartBegin() {
                    @Override
                    public WritableByteChannel apply(UploadContext context, ByteBuffer buffer) throws IOException {
                        PartStream part = context.getCurrentPart();
                        Path path = uploadFilePath.resolve(part.getSubmittedFileName());
                        return Channels.newChannel(Files.newOutputStream(path));
                    }
                })
                .onPartEnd(new OnPartEnd() {
                    @Override
                    public void accept(UploadContext context, WritableByteChannel channel) throws IOException {
                        channel.close();
                    }
                })
                .setup();
    }
}
