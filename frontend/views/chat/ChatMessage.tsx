// components/ChatMessage.tsx
import ReactMarkdown from 'react-markdown';
import rehypeHighlight from 'rehype-highlight';
import 'highlight.js/styles/atom-one-light.css';
import ChatCompletionMessage from "Frontend/generated/com/azure/spring/movee/model/ChatCompletionMessage";
import Role from "Frontend/generated/com/azure/spring/movee/model/ChatCompletionMessage/Role";

export default function ChatMessage({ content, role }: ChatCompletionMessage) {
    console.log("content: "+content);
  return (
      <div className="cs-message__content-wrapper">
        {role == Role.USER ? (
            <div className="cs-outgoing-message max-w-full overflow-x-scroll">
              <ReactMarkdown rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]}>{content || ''}</ReactMarkdown>
            </div>
        ) : (
            <div className="cs-incoming-message max-w-full overflow-x-scroll">
              <ReactMarkdown rehypePlugins={[[rehypeHighlight, { ignoreMissing: true }]]}>{content || ''}</ReactMarkdown>
            </div>
            )}
      </div>
  );
}
