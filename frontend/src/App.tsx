import { BrowserRouter as Router, Routes, Route, Navigate } from 'react-router-dom';
import Navigation from './components/Navigation';
import CollectionPage from './pages/CollectionPage';
import DiscoverPage from './pages/DiscoverPage';

function App() {
  return (
    <Router>
      <div className="app">
        <Navigation />
        <main className="app-main">
          <Routes>
            <Route path="/" element={<Navigate to="/collection" replace />} />
            <Route path="/collection" element={<CollectionPage />} />
            <Route path="/discover" element={<DiscoverPage />} />
          </Routes>
        </main>
      </div>
    </Router>
  );
}

export default App;
