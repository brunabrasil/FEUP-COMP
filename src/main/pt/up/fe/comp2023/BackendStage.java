package pt.up.fe.comp2023;

import java.util.ArrayList;
import java.util.List;

import org.specs.comp.ollir.ClassUnit;

import pt.up.fe.comp.jmm.jasmin.JasminBackend;
import pt.up.fe.comp.jmm.jasmin.JasminResult;
import pt.up.fe.comp.jmm.ollir.OllirResult;
import pt.up.fe.comp.jmm.report.Report;

public class BackendStage implements JasminBackend {

    @Override
    public JasminResult toJasmin(OllirResult ollirResult) {
        // Retrieve the root node of the OLLIR tree
        ClassUnit ollirClass = ollirResult.getOllirClass();

        String jasminCode = new JasminGenerator(ollirClass).dealWithClass();

        // More reports from this stage
        List<Report> reports = new ArrayList<>();

        return new JasminResult(ollirResult, jasminCode, reports);
    }
}