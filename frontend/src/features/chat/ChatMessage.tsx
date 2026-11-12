import React from 'react';
import './ChatMessage.css';

interface ChatMessageProps {
  role: 'user' | 'assistant';
  content: React.ReactNode;
}

export const ChatMessage: React.FC<ChatMessageProps> = ({ role, content }) => {
  return (
    <div className={`chat-message chat-message--${role}`}>
      <div className="chat-message__content">
        {content}
      </div>
    </div>
  );
};
