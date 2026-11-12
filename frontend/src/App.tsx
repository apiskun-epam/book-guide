import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navigation from './components/Navigation';
import CollectionPage from './pages/CollectionPage';
import ChatPage from './features/chat/ChatPage';
import { ToastProvider } from './components/ToastProvider';

function App() {
  return (
    <ToastProvider>
    <Router>
      <div className="app">
        <Navigation />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<Navigate to="/collection" replace />} />
            <Route path="/collection" element={<CollectionPage />} />
            <Route path="/discover" element={<ChatPage />} />
          </Routes>
        </main>
      </div>
    </Router>
    </ToastProvider>
  );
}

export default App;
