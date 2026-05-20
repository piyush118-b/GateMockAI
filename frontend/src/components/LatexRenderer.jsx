import React from 'react';
import katex from 'katex';
import 'katex/dist/katex.min.css';

export default function LatexRenderer({ text = '' }) {
  if (!text) return null;

  // Split text by block math $$...$$
  const parts = text.split(/(\$\$[\s\S]*?\$\$)/g);

  return (
    <span>
      {parts.map((part, index) => {
        if (part.startsWith('$$') && part.endsWith('$$')) {
          const math = part.slice(2, -2);
          try {
            const html = katex.renderToString(math, { displayMode: true, throwOnError: false });
            return <span key={index} dangerouslySetInnerHTML={{ __html: html }} className="block my-2 overflow-x-auto" />;
          } catch (e) {
            return <code key={index}>{math}</code>;
          }
        }
        
        // Inside each non-block part, split by inline math $...$
        const subParts = part.split(/(\$[\s\S]*?\$)/g);
        return (
          <span key={index}>
            {subParts.map((subPart, subIndex) => {
              if (subPart.startsWith('$') && subPart.endsWith('$')) {
                const math = subPart.slice(1, -1);
                try {
                  const html = katex.renderToString(math, { displayMode: false, throwOnError: false });
                  return <span key={subIndex} dangerouslySetInnerHTML={{ __html: html }} className="inline-block px-0.5" />;
                } catch (e) {
                  return <code key={subIndex}>{math}</code>;
                }
              }
              return <span key={subIndex}>{subPart}</span>;
            })}
          </span>
        );
      })}
    </span>
  );
}
