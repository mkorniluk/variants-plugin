package variants.actions;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.json.JSONArray;
import org.json.JSONObject;

public class VariantsProperties {
    class Variant {
        public String name;
        public List<String> srcFolders = new ArrayList<String>();
        public List<String> resFolders = new ArrayList<String>();

        public Variant(String name) {
            this.name = name;
        }
    }

    List<Variant> variants = new ArrayList<Variant>();

    public void read(ArrayList<String> tokens) throws Exception {
        while (tokens.size() > 0) {
            String token = tokens.remove(0);
            Variant variant = new Variant(token);
            readVariant(variant, tokens);
            variants.add(variant);
        }
    }

    private void readVariant(Variant variant, ArrayList<String> tokens) throws Exception {
        String token = tokens.remove(0);
        if (!token.equals("{"))
            throw new Exception("'{' expected");
        while (tokens.size() > 0) {
            token = tokens.remove(0);
            if (token.equals("}")) {
                return;
            } else if (token.equals("src")) {
                readSrc(variant, tokens);
            } else if (token.equals("res")) {
                readRes(variant, tokens);
            } else {
                throw new Exception("res, src or '}' expected");
            }
        }
    }

    private void readRes(Variant variant, ArrayList<String> tokens) throws Exception {
        String token = tokens.remove(0);
        if (!token.equals("{"))
            throw new Exception("'{' expected");
        while (tokens.size() > 0) {
            token = tokens.remove(0);
            if (token.equals("}")) {
                return;
            } else {
                variant.resFolders.add(token);
            }
        }
    }

    private void readSrc(Variant variant, ArrayList<String> tokens) throws Exception {
        String token = tokens.remove(0);
        if (!token.equals("{"))
            throw new Exception("'{' expected");
        while (tokens.size() > 0) {
            token = tokens.remove(0);
            if (token.equals("}")) {
                return;
            } else {
                variant.srcFolders.add(token);
            }
        }
    }

    public void read(IFile file) {
        try {
            BufferedReader stream = new BufferedReader(new InputStreamReader(file.getContents()));
            StringBuilder stringBuilder = new StringBuilder();
            try {
                while (true) {
                    String line = stream.readLine();
                    if (line == null)
                        break;
                    stringBuilder.append(line);
                }
            } catch (IOException ex) {

            }
            stream.close();
            String configuration = stringBuilder.toString();
            /*
             * StringTokenizer st = new StringTokenizer(configuration, "\n{}\t",
             * true); ArrayList<String> tokens = new ArrayList<>(); while
             * (st.hasMoreTokens()) { String token = st.nextToken(); if
             * (token.equals("\n") || token.equals("\t")) continue;
             * tokens.add(token.trim()); } read(tokens);
             */
            JSONObject json = new JSONObject("{" + configuration + "}");
            String[] variantsNames = JSONObject.getNames(json);
            for (String variantName : variantsNames) {
                JSONObject variantJSON = json.getJSONObject(variantName);
                Variant v = new Variant(variantName);
                JSONArray src = variantJSON.getJSONArray("src");
                if (src != null) {
                    for (int i = 0; i < src.length(); i++) {
                        v.srcFolders.add(src.getString(i));
                    }
                }
                JSONArray res = variantJSON.getJSONArray("res");
                if (res != null) {
                    for (int i = 0; i < res.length(); i++) {
                        v.resFolders.add(res.getString(i));
                    }
                }
                variants.add(v);
            }
        } catch (Exception e) {

        }
    }
}
