package com.ntrepid.tartan;

import java.io.*;
import java.util.*;
import java.util.zip.*;

import javax.servlet.*;
import javax.servlet.http.*;

import edu.stanford.nlp.ie.*;
import edu.stanford.nlp.ie.crf.*;

public class NERServlet extends HttpServlet
{
    private String format;
    private boolean spacing;
    private String default_classifier;
    private String[] classifiers;
    private HashMap<String, AbstractSequenceClassifier> ners;

    public void init() throws ServletException {
        format = getServletConfig().getInitParameter("outputFormat");
        if (format == null || format.trim().equals(""))
            throw new ServletException("Invalid outputFormat setting.");
        
        String spacingStr = getServletConfig().getInitParameter("preserveSpacing");
        if (spacingStr == null || spacingStr.trim().equals(""))
            throw new ServletException("Invalid preserveSpacing setting.");
        //spacing = Boolean.valueOf(spacingStr).booleanValue();
        spacingStr = spacingStr.trim().toLowerCase();
        spacing = "true".equals(spacingStr);
        
        default_classifier = getServletConfig().getInitParameter("default-classifier");
        if (default_classifier == null || default_classifier.trim().equals(""))
            throw new ServletException("Default classifier not given.");
        
        String classifiersStr = getServletConfig().getInitParameter("classifiers");
        if (classifiersStr == null || classifiersStr.trim().equals(""))
            throw new ServletException("List of classifiers not given.");
        classifiers = classifiersStr.split("\\s+");
        
        ners = new HashMap<String, AbstractSequenceClassifier>();
        for (String classifier : classifiers) {
            AbstractSequenceClassifier asc = null;
            String filename = getServletConfig().getInitParameter(classifier);
            InputStream is = getServletConfig().getServletContext().getResourceAsStream(filename);

            if (is == null)
                throw new ServletException("File not found. Filename = " + filename);
            try {
                if (filename.endsWith(".gz")) {
                    is = new BufferedInputStream(new GZIPInputStream(is));
                } else {
                    is = new BufferedInputStream(is);
                }
                asc = CRFClassifier.getClassifier(is);
            } catch (IOException e) {
                throw new ServletException("IO problem reading classifier.");
            } catch (ClassCastException e) {
                throw new ServletException("Classifier class casting problem.");
            } catch (ClassNotFoundException e) {
                throw new ServletException("Clasifier class not found problem.");
            } finally {
                try {
                    is.close();
                } catch (IOException e) {
                    //do nothing
                }
            }
            ners.put(classifier, asc);
        }
    }

    public void doGet(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        doPost(req, res);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
            throws ServletException, IOException {
        String input = req.getParameter("input");
        
        String outputFormat = req.getParameter("outputFormat");
        if (outputFormat == null || outputFormat.trim().equals("")) {
            outputFormat = this.format;
        }
        
        boolean preserveSpacing;
        String preserveSpacingStr = req.getParameter("preserveSpacing");
        if (preserveSpacingStr == null || preserveSpacingStr.trim().equals("")) {
            preserveSpacing = this.spacing;
        } else {
            //preserveSpacing = Boolean.getBoolean(preserveSpacingStr);
            preserveSpacingStr = preserveSpacingStr.trim().toLowerCase();
            preserveSpacing = "true".equals(preserveSpacingStr);
        }
        
        String classifier = req.getParameter("classifier");
        if (classifier == null || classifier.trim().equals("")) {
            classifier = this.default_classifier;
        }

        res.setContentType("text/plain");
        res.addHeader("classifier", classifier);
        res.addHeader("outputFormat", outputFormat);
        res.addHeader("preserveSpacing", String.valueOf(preserveSpacing));
        PrintWriter out = res.getWriter();
        out.print(ners.get(classifier).classifyToString(input, outputFormat, preserveSpacing));
    }
}