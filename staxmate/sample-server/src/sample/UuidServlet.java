package sample;

import java.io.*;
import javax.servlet.http.*;

/**
 * This is the simple UUID servlet, implemented using StaxMate XML
 * library and JUG Uuid Generator.
 */
public class UuidServlet extends HttpServlet
{
    public UuidServlet() { }

    public void doGet(HttpServletRequest request,
                      HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/xml");

        PrintWriter out = response.getWriter();

        out.println("<get>GET!</get>\n");
    }

    public void doPost(HttpServletRequest request,
                       HttpServletResponse response)
        throws IOException
    {
        response.setContentType("text/xml");

        PrintWriter out = response.getWriter();

        out.println("<post>POST!</post>\n");
    }
}

