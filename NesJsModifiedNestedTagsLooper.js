import React, { useState } from "react";
import { Textarea } from "@/components/ui/textarea";
import { Button } from "@/components/ui/button";
import { Card, CardContent } from "@/components/ui/card";

function RuleEditor({ rules, setRules, fieldSuggestions, path = "" }) {
  // ...component code to handle rule creation (same as above)
}

export default function XmlTransformRuleBuilder() {
  const [xmlInput, setXmlInput] = useState("");
  const [fieldSuggestions, setFieldSuggestions] = useState([]);
  const [rules, setRules] = useState([]);
  const [metadataJson, setMetadataJson] = useState("");

  const handleExtractFields = () => {
    const parser = new DOMParser();
    const xml = parser.parseFromString(xmlInput, "text/xml");

    const paths = new Set();

    const traverse = (node, currentPath) => {
      if (node.nodeType === Node.ELEMENT_NODE) {
        const newPath = `${currentPath}/${node.nodeName}`;
        paths.add(newPath);
        Array.from(node.attributes || []).forEach(attr => {
          paths.add(`${newPath}/@${attr.name}`);
        });
        Array.from(node.childNodes).forEach(child => traverse(child, newPath));
      }
    };

    traverse(xml.documentElement, "");
    setFieldSuggestions([...paths].sort());
  };

  const generateMetadata = () => {
    const rootTag = xmlInput.match(/<([a-zA-Z0-9_]+)>/);
    const metadata = {
      rootXPath: rootTag ? `/${rootTag[1]}` : "/root",
      iterateXPath: `./${rootTag ? rootTag[1].slice(0, -1) : "item"}`,
      outputFormat: "json",
      mappings: rules
    };
    setMetadataJson(JSON.stringify(metadata, null, 2));
  };

  return (
    <div className="p-4 space-y-4">
      <h1 className="text-xl font-bold">XML Transform Rule Builder</h1>
      <Card>
        <CardContent className="p-4 space-y-2">
          <Textarea
            rows={10}
            placeholder="Paste your XML here"
            value={xmlInput}
            onChange={(e) => setXmlInput(e.target.value)}
          />
          <Button onClick={handleExtractFields}>Extract Fields</Button>
        </CardContent>
      </Card>

      <Card>
        <CardContent className="p-4 space-y-4">
          <h2 className="text-lg font-semibold">Mapping Rules</h2>
          <RuleEditor
            rules={rules}
            setRules={setRules}
            fieldSuggestions={fieldSuggestions}
          />
          <Button onClick={generateMetadata}>Generate Metadata JSON</Button>
        </CardContent>
      </Card>

      {metadataJson && (
        <Card>
          <CardContent className="p-4">
            <h2 className="text-lg font-semibold">Generated Metadata JSON</h2>
            <Textarea rows={10} value={metadataJson} readOnly className="w-full" />
          </CardContent>
        </Card>
      )}
    </div>
  );
}
