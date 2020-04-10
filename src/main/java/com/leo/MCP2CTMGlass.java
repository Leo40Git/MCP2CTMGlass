package com.leo;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.apache.commons.cli.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;

public class MCP2CTMGlass {

    private static BufferedImage stitchCTMTexture(String srcPath) throws IOException {
        File[] files = new File[4];
        files[0] = new File(srcPath + "/26.png");
        files[1] = new File(srcPath + "/24.png");
        files[2] = new File(srcPath + "/2.png");
        files[3] = new File(srcPath + "/46.png");
        BufferedImage[] texs = new BufferedImage[files.length];
        for (int i = 0; i < files.length; i++) {
            File file = files[i];
            if (!file.exists())
                throw new FileNotFoundException(file.toString());
            texs[i] = ImageIO.read(file);
        }
        int w = texs[0].getWidth();
        int h = texs[0].getHeight();
        BufferedImage result = new BufferedImage(w * 2, h * 2, texs[0].getType());
        Graphics g = result.getGraphics();
        for (int i = 0; i < files.length; i++)
            g.drawImage(texs[i], (i % 2) * w, (i / 2) * h, null);
        g.dispose();
        return result;
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private static void writeCTMProperties(String dstFile, String color) throws IOException {
        JsonObject root = new JsonObject();
        root.addProperty("__comment", "Generated using ADudeCalledLeo's MCP2CTMGlass tool");
        JsonObject ctm = new JsonObject();
        ctm.addProperty("ctm_version", 1);
        ctm.addProperty("type", "CTM");
        ctm.addProperty("layer", (color != null ? "TRANSLUCENT" : "CUTOUT"));
        JsonArray textures = new JsonArray();
        String texName = "glass";
        if (color != null)
            texName += "_" + color;
        texName += "_ctm.png";
        textures.add(texName);
        ctm.add("textures", textures);
        root.add("ctm", ctm);
        FileWriter fw = new FileWriter(dstFile);
        GSON.toJson(root, fw);
        fw.close();
    }

    public static void main(String[] args) {
        Options options = new Options();

        Option input = new Option("i", "input", true, "input directory");
        input.setRequired(true);
        options.addOption(input);

        Option output = new Option("o", "output", true, "output directory");
        output.setRequired(true);
        options.addOption(output);

        Option noStained = new Option("nostained", false, "if specified, skips processing stained glass");
        options.addOption(noStained);

        CommandLineParser parser = new DefaultParser();
        HelpFormatter formatter = new HelpFormatter();
        CommandLine cmd = null;

        try {
            cmd = parser.parse(options, args);
        } catch (ParseException e) {
            System.out.println(e.getMessage());
            formatter.printHelp("mcp2ctmglass", options);
            System.exit(1);
        }

        String inPath = cmd.getOptionValue("input");
        String outPath = cmd.getOptionValue("output");
        boolean skipStained = cmd.hasOption("no-stained");

        System.out.println("Processing standard glass...");
        try {
            BufferedImage ctm = stitchCTMTexture(inPath + "/aregular");
            ImageIO.write(ctm, "png", new File(outPath + "/glass_ctm.png"));
            writeCTMProperties(outPath + "/glass.png.mcmeta", null);
        } catch (IOException e) {
            System.out.println("Failed to write CTM data for standard glass:");
            e.printStackTrace(System.out);
        }

        if (!skipStained) {
            final String[] colors = {
                    "black",
                    "blue",
                    "brown",
                    "cyan",
                    "gray",
                    "green",
                    "lightblue",
                    "lime",
                    "magenta",
                    "orange",
                    "pink",
                    "purple",
                    "red",
                    "silver",
                    "white",
                    "yellow"
            };

            for (String color : colors) {
                System.out.println("Processing stained glass of color " + color + "...");
                try {
                    BufferedImage ctm = stitchCTMTexture(inPath + "/" + color);
                    ImageIO.write(ctm, "png", new File(outPath + "/glass_" + color + "_ctm.png"));
                    writeCTMProperties(outPath + "/glass_" + color + ".png.mcmeta", color);
                } catch (IOException e) {
                    System.out.println("Failed to write CTM data for stained glass of color " + color);
                    e.printStackTrace(System.out);
                }
            }
        }

        System.out.println("Done.");
    }

}
