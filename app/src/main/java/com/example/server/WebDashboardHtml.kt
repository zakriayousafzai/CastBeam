package com.example.server

object WebDashboardHtml {

    fun generateHtml(): String {
        val dollar = "$"
        return """
<!DOCTYPE html>
<html lang="en">
<head>
  <meta charset="UTF-8">
  <meta name="viewport" content="width=device-width, initial-scale=1.0">
  <title>CastBeam - Local Media Hub</title>
  <style>
    :root {
      --bg: #090D16;
      --surface: #0F172A;
      --surface-card: #162238;
      --surface-focus: #1E2D4A;
      --border: #334155;
      --primary: #38BDF8;
      --primary-hover: #7DD3FC;
      --amber: #F59E0B;
      --text-main: #F8FAFC;
      --text-muted: #94A3B8;
      --success: #10B981;
    }
    * {
      box-sizing: border-box;
      margin: 0;
      padding: 0;
      font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif;
    }
    body {
      background-color: var(--bg);
      color: var(--text-main);
      min-height: 100vh;
      display: flex;
      flex-direction: column;
      overflow-x: hidden;
    }
    header {
      background: rgba(15, 23, 42, 0.95);
      backdrop-filter: blur(12px);
      border-bottom: 1px solid var(--border);
      padding: 16px 28px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      position: sticky;
      top: 0;
      z-index: 100;
      flex-wrap: wrap;
      gap: 16px;
    }
    .brand {
      display: flex;
      align-items: center;
      gap: 14px;
      text-decoration: none;
      color: var(--text-main);
    }
    .brand-icon {
      width: 44px;
      height: 44px;
      background: linear-gradient(135deg, #0284C7, #0369A1);
      border-radius: 12px;
      display: flex;
      align-items: center;
      justify-content: center;
      box-shadow: 0 0 16px rgba(56, 189, 248, 0.35);
      border: 1px solid rgba(56, 189, 248, 0.4);
    }
    .brand-title {
      font-size: 24px;
      font-weight: 800;
      letter-spacing: -0.5px;
      display: flex;
      align-items: center;
      gap: 8px;
    }
    .brand-badge {
      font-size: 11px;
      background: rgba(56, 189, 248, 0.15);
      color: var(--primary);
      padding: 3px 8px;
      border-radius: 20px;
      font-weight: 600;
      border: 1px solid rgba(56, 189, 248, 0.3);
    }
    .header-actions {
      display: flex;
      align-items: center;
      gap: 12px;
    }
    .search-bar {
      background: var(--surface);
      border: 1px solid var(--border);
      border-radius: 24px;
      padding: 8px 18px;
      color: #fff;
      font-size: 14px;
      width: 240px;
      transition: all 0.2s;
    }
    .search-bar:focus {
      outline: none;
      border-color: var(--primary);
      box-shadow: 0 0 0 2px rgba(56, 189, 248, 0.2);
      width: 280px;
    }
    .btn {
      background: var(--surface-card);
      color: var(--text-main);
      border: 1px solid var(--border);
      padding: 10px 18px;
      border-radius: 24px;
      font-size: 14px;
      font-weight: 600;
      cursor: pointer;
      display: inline-flex;
      align-items: center;
      gap: 8px;
      transition: all 0.2s;
    }
    .btn:hover, .btn:focus {
      background: var(--surface-focus);
      border-color: var(--primary);
      outline: none;
    }
    .btn-primary {
      background: linear-gradient(135deg, #0284C7, #0369A1);
      color: #fff;
      border-color: transparent;
    }
    .btn-primary:hover, .btn-primary:focus {
      background: linear-gradient(135deg, #0369A1, #075985);
      box-shadow: 0 0 12px rgba(56, 189, 248, 0.4);
    }
    .sync-status {
      display: flex;
      align-items: center;
      gap: 6px;
      font-size: 13px;
      color: var(--text-muted);
      background: rgba(16, 185, 129, 0.1);
      padding: 6px 12px;
      border-radius: 20px;
      border: 1px solid rgba(16, 185, 129, 0.2);
    }
    .pulse-dot {
      width: 8px;
      height: 8px;
      border-radius: 50%;
      background-color: var(--success);
      animation: pulse 2s infinite;
    }
    @keyframes pulse {
      0% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0.7); }
      70% { transform: scale(1); box-shadow: 0 0 0 6px rgba(16, 185, 129, 0); }
      100% { transform: scale(0.95); box-shadow: 0 0 0 0 rgba(16, 185, 129, 0); }
    }
    .category-nav {
      padding: 12px 28px;
      display: flex;
      gap: 10px;
      overflow-x: auto;
      border-bottom: 1px solid rgba(51, 65, 85, 0.4);
    }
    .cat-pill {
      background: var(--surface);
      border: 1px solid var(--border);
      color: var(--text-muted);
      padding: 8px 16px;
      border-radius: 20px;
      font-size: 13px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      transition: all 0.2s;
    }
    .cat-pill.active, .cat-pill:focus {
      background: var(--primary);
      color: #090D16;
      border-color: var(--primary);
      outline: none;
    }
    main {
      flex: 1;
      padding: 24px 28px;
      max-width: 1600px;
      width: 100%;
      margin: 0 auto;
    }
    .tv-tips-banner {
      background: linear-gradient(90deg, rgba(30, 41, 59, 0.8), rgba(15, 23, 42, 0.8));
      border: 1px solid var(--border);
      border-left: 4px solid var(--amber);
      border-radius: 12px;
      padding: 14px 20px;
      margin-bottom: 24px;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 16px;
    }
    .tv-tips-content {
      font-size: 14px;
      color: var(--text-muted);
    }
    .tv-tips-content strong {
      color: var(--text-main);
    }
    .kbd {
      background: rgba(0,0,0,0.5);
      border: 1px solid var(--border);
      border-radius: 4px;
      padding: 2px 6px;
      font-size: 11px;
      color: var(--amber);
      font-family: monospace;
    }
    .media-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(280px, 1fr));
      gap: 22px;
    }
    .media-card {
      background: var(--surface-card);
      border: 1px solid var(--border);
      border-radius: 14px;
      overflow: hidden;
      cursor: pointer;
      display: flex;
      flex-direction: column;
      position: relative;
      transition: transform 0.2s, box-shadow 0.2s, border-color 0.2s;
    }
    .media-card:hover, .media-card:focus, .media-card.focused {
      transform: translateY(-4px) scale(1.02);
      box-shadow: 0 12px 24px rgba(0, 0, 0, 0.6), 0 0 0 2px var(--primary);
      border-color: var(--primary);
      outline: none;
    }
    .card-preview {
      height: 160px;
      background: #0B1120;
      display: flex;
      align-items: center;
      justify-content: center;
      position: relative;
    }
    .card-icon {
      font-size: 56px;
      opacity: 0.85;
    }
    .card-badge {
      position: absolute;
      bottom: 10px;
      right: 10px;
      background: rgba(0, 0, 0, 0.8);
      color: #fff;
      padding: 4px 8px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 700;
      letter-spacing: 0.5px;
    }
    .card-folder {
      position: absolute;
      top: 10px;
      left: 10px;
      background: rgba(15, 23, 42, 0.85);
      border: 1px solid rgba(51, 65, 85, 0.6);
      color: var(--text-muted);
      padding: 2px 8px;
      border-radius: 12px;
      font-size: 11px;
    }
    .card-body {
      padding: 16px;
      display: flex;
      flex-direction: column;
      flex: 1;
    }
    .card-title {
      font-size: 16px;
      font-weight: 700;
      line-height: 1.3;
      margin-bottom: 8px;
      word-break: break-word;
      display: -webkit-box;
      -webkit-line-clamp: 2;
      -webkit-box-orient: vertical;
      overflow: hidden;
    }
    .card-meta {
      font-size: 12px;
      color: var(--text-muted);
      margin-top: auto;
      display: flex;
      justify-content: space-between;
      align-items: center;
    }
    .card-actions {
      display: flex;
      gap: 8px;
      margin-top: 14px;
    }
    .btn-sm {
      padding: 6px 12px;
      font-size: 12px;
      border-radius: 16px;
      flex: 1;
      justify-content: center;
    }
    /* Player Overlay Modal */
    .player-modal {
      position: fixed;
      top: 0;
      left: 0;
      width: 100vw;
      height: 100vh;
      background: rgba(0, 0, 0, 0.96);
      z-index: 1000;
      display: none;
      flex-direction: column;
    }
    .player-modal.active {
      display: flex;
    }
    .player-header {
      padding: 16px 24px;
      display: flex;
      justify-content: space-between;
      align-items: center;
      background: linear-gradient(180deg, rgba(0,0,0,0.8), transparent);
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      z-index: 20;
    }
    .player-title {
      font-size: 18px;
      font-weight: 700;
      color: #fff;
      display: flex;
      align-items: center;
      gap: 10px;
    }
    .player-container {
      flex: 1;
      display: flex;
      align-items: center;
      justify-content: center;
      background: #000;
      position: relative;
    }
    video {
      width: 100%;
      height: 100%;
      max-height: 100vh;
      object-fit: contain;
    }
    /* Remote Deck Indicator on TV */
    .remote-toast {
      position: fixed;
      bottom: 24px;
      left: 50%;
      transform: translateX(-50%);
      background: rgba(15, 23, 42, 0.95);
      border: 1px solid var(--primary);
      padding: 10px 24px;
      border-radius: 30px;
      color: #fff;
      font-size: 14px;
      font-weight: 600;
      display: none;
      align-items: center;
      gap: 10px;
      box-shadow: 0 8px 24px rgba(0,0,0,0.6);
      z-index: 2000;
    }
    .empty-state {
      text-align: center;
      padding: 60px 20px;
      color: var(--text-muted);
    }
    .empty-icon {
      font-size: 64px;
      margin-bottom: 16px;
      opacity: 0.6;
    }
  </style>
</head>
<body>
  <header>
    <a href="/" class="brand">
      <div class="brand-icon">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="#fff" stroke-width="2.5">
          <path d="M4 6h16M4 12h16M4 18h9M18 15l4 3-4 3v-6z"/>
        </svg>
      </div>
      <div>
        <div class="brand-title">
          CastBeam
          <span class="brand-badge">Local Hub</span>
        </div>
      </div>
    </a>

    <div class="header-actions">
      <div class="sync-status">
        <div class="pulse-dot"></div>
        <span>Phone Synced</span>
      </div>
      <input type="text" id="searchInput" class="search-bar" placeholder="Search movies, files..." />
      <button class="btn" id="uploadBtn" onclick="triggerUpload()">
        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
          <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
        </svg>
        Upload File
      </button>
      <input type="file" id="fileUploadInput" style="display:none;" onchange="handleFileUpload(event)">
    </div>
  </header>

  <div class="category-nav">
    <button class="cat-pill active" onclick="filterCategory('ALL')">All Files (<span id="countAll">0</span>)</button>
    <button class="cat-pill" onclick="filterCategory('VIDEO')">🎬 Movies & Videos (<span id="countVideo">0</span>)</button>
    <button class="cat-pill" onclick="filterCategory('AUDIO')">🎵 Audio & Music (<span id="countAudio">0</span>)</button>
    <button class="cat-pill" onclick="filterCategory('IMAGE')">🖼️ Photos (<span id="countImage">0</span>)</button>
    <button class="cat-pill" onclick="filterCategory('DOCUMENT')">📄 Documents (<span id="countDoc">0</span>)</button>
  </div>

  <main>
    <div class="tv-tips-banner">
      <div class="tv-tips-content">
        📺 <strong>Smart TV Navigation:</strong> Use your remote D-pad <span class="kbd">▲</span> <span class="kbd">▼</span> <span class="kbd">◄</span> <span class="kbd">►</span> and <span class="kbd">OK</span> to navigate. In player: <span class="kbd">Space</span> = Play/Pause, <span class="kbd">◄</span> <span class="kbd">►</span> = Seek 10s, <span class="kbd">F</span> = Fullscreen.
      </div>
    </div>

    <div id="mediaGrid" class="media-grid"></div>

    <div id="emptyState" class="empty-state" style="display:none;">
      <div class="empty-icon">📁</div>
      <h2>No media files found in this category</h2>
      <p style="margin-top: 8px;">Select a folder in the CastBeam Android app on your phone to share files.</p>
    </div>
  </main>

  <!-- Video & Media Player Modal -->
  <div id="playerModal" class="player-modal">
    <div class="player-header">
      <div class="player-title">
        <span>🎬</span>
        <span id="playerMediaTitle">Movie Title</span>
      </div>
      <div>
        <button class="btn btn-primary" onclick="closePlayer()">✕ Close (Esc)</button>
      </div>
    </div>
    <div class="player-container">
      <video id="tvVideoPlayer" controls autoplay playsinline preload="auto"></video>
    </div>
  </div>

  <!-- Real-time remote command toast notification -->
  <div id="remoteToast" class="remote-toast">
    <span>📱</span>
    <span id="remoteToastText">Phone sent command</span>
  </div>

  <script>
    var allMedia = [];
    var currentCategory = 'ALL';
    var focusedIndex = 0;
    var playerModal = document.getElementById('playerModal');
    var tvVideoPlayer = document.getElementById('tvVideoPlayer');
    var playerMediaTitle = document.getElementById('playerMediaTitle');
    var searchInput = document.getElementById('searchInput');
    var remoteToast = document.getElementById('remoteToast');
    var remoteToastText = document.getElementById('remoteToastText');

    var currentPlayingMedia = null;

    async function loadMedia() {
      try {
        var res = await fetch('/api/media');
        allMedia = await res.json();
        updateCategoryCounts();
        renderMedia();
      } catch (e) {
        console.error('Failed to load media catalog:', e);
      }
    }

    function updateCategoryCounts() {
      document.getElementById('countAll').innerText = allMedia.length;
      document.getElementById('countVideo').innerText = allMedia.filter(function(m) { return m.category === 'VIDEO'; }).length;
      document.getElementById('countAudio').innerText = allMedia.filter(function(m) { return m.category === 'AUDIO'; }).length;
      document.getElementById('countImage').innerText = allMedia.filter(function(m) { return m.category === 'IMAGE'; }).length;
      document.getElementById('countDoc').innerText = allMedia.filter(function(m) { return m.category === 'DOCUMENT'; }).length;
    }

    function filterCategory(cat) {
      currentCategory = cat;
      document.querySelectorAll('.cat-pill').forEach(function(btn) {
        btn.classList.toggle('active', btn.innerText.indexOf(cat) !== -1 || (cat === 'ALL' && btn.innerText.indexOf('All') !== -1));
      });
      renderMedia();
    }

    searchInput.addEventListener('input', function() {
      renderMedia();
    });

    function renderMedia() {
      var grid = document.getElementById('mediaGrid');
      var emptyState = document.getElementById('emptyState');
      var query = searchInput.value.toLowerCase().trim();

      var filtered = allMedia.filter(function(m) {
        var matchesCat = (currentCategory === 'ALL' || m.category === currentCategory);
        var matchesQuery = !query || m.name.toLowerCase().indexOf(query) !== -1 || (m.folderName && m.folderName.toLowerCase().indexOf(query) !== -1);
        return matchesCat && matchesQuery;
      });

      if (filtered.length === 0) {
        grid.innerHTML = '';
        emptyState.style.display = 'block';
        return;
      }
      emptyState.style.display = 'none';

      var html = '';
      filtered.forEach(function(item, idx) {
        var icon = getCategoryIcon(item.category);
        var durationTag = item.formattedDuration ?
          '<span class="card-badge">' + item.formattedDuration + '</span>' :
          '<span class="card-badge">' + item.formattedSize + '</span>';

        html += '<div class="media-card" tabindex="0" data-idx="' + idx + '" onclick="playMediaById(\'' + item.id + '\')">' +
          '<div class="card-preview">' +
            '<span class="card-folder">' + escapeHtml(item.folderName || 'Storage') + '</span>' +
            '<div class="card-icon">' + icon + '</div>' +
            durationTag +
          '</div>' +
          '<div class="card-body">' +
            '<div class="card-title">' + escapeHtml(item.name) + '</div>' +
            '<div class="card-meta">' +
              '<span>' + item.formattedSize + '</span>' +
              '<span>' + item.category + '</span>' +
            '</div>' +
            '<div class="card-actions">' +
              '<button class="btn btn-primary btn-sm" onclick="event.stopPropagation(); playMediaById(\'' + item.id + '\')">' +
                '▶ Play' +
              '</button>' +
              '<a href="/download?id=' + item.id + '" download class="btn btn-sm" onclick="event.stopPropagation()">' +
                '⬇ Download' +
              '</a>' +
            '</div>' +
          '</div>' +
        '</div>';
      });
      grid.innerHTML = html;
    }

    function getCategoryIcon(cat) {
      switch (cat) {
        case 'VIDEO': return '🎬';
        case 'AUDIO': return '🎵';
        case 'IMAGE': return '🖼️';
        case 'DOCUMENT': return '📄';
        default: return '📁';
      }
    }

    function playMediaById(id) {
      var item = allMedia.find(function(m) { return m.id === id; });
      if (!item) return;

      currentPlayingMedia = item;
      playerMediaTitle.innerText = item.name;
      tvVideoPlayer.src = '/stream?id=' + encodeURIComponent(id);

      while (tvVideoPlayer.firstChild) {
        tvVideoPlayer.removeChild(tvVideoPlayer.firstChild);
      }

      var track = document.createElement('track');
      track.kind = 'subtitles';
      track.label = 'Subtitles';
      track.srclang = 'en';
      track.src = '/subtitles?id=' + encodeURIComponent(id);
      track.default = true;
      tvVideoPlayer.appendChild(track);

      playerModal.classList.add('active');
      tvVideoPlayer.play().catch(function(e) { console.log('Autoplay prevented:', e); });

      reportPlayerStatus();
    }

    function closePlayer() {
      tvVideoPlayer.pause();
      tvVideoPlayer.src = '';
      playerModal.classList.remove('active');
      currentPlayingMedia = null;
      reportPlayerStatus();
    }

    setInterval(reportPlayerStatus, 800);

    async function reportPlayerStatus() {
      try {
        var payload = {
          mediaId: currentPlayingMedia ? currentPlayingMedia.id : null,
          title: currentPlayingMedia ? currentPlayingMedia.name : "Idle",
          currentTime: tvVideoPlayer.currentTime || 0,
          duration: tvVideoPlayer.duration || 0,
          isPlaying: !tvVideoPlayer.paused && !tvVideoPlayer.ended,
          volume: tvVideoPlayer.volume || 1.0,
          isMuted: tvVideoPlayer.muted || false
        };
        await fetch('/api/remote/status', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify(payload)
        });
      } catch (e) {
      }
    }

    setInterval(pollRemoteCommands, 500);

    async function pollRemoteCommands() {
      try {
        var res = await fetch('/api/remote/state');
        if (!res.ok) return;
        var data = await res.json();
        
        if (data.commands && data.commands.length > 0) {
          data.commands.forEach(function(cmd) { executeRemoteCommand(cmd); });
        }
      } catch (e) {
      }
    }

    function showRemoteToast(text) {
      remoteToastText.innerText = text;
      remoteToast.style.display = 'flex';
      setTimeout(function() {
        remoteToast.style.display = 'none';
      }, 2000);
    }

    function executeRemoteCommand(cmd) {
      console.log('Executing remote command:', cmd);
      switch (cmd.action) {
        case 'play':
          tvVideoPlayer.play();
          showRemoteToast('▶ Playing via Phone');
          break;
        case 'pause':
          tvVideoPlayer.pause();
          showRemoteToast('⏸ Paused via Phone');
          break;
        case 'toggle_play':
          if (tvVideoPlayer.paused) tvVideoPlayer.play(); else tvVideoPlayer.pause();
          showRemoteToast('⏯ Play/Pause toggled');
          break;
        case 'seek':
          if (cmd.value !== null) {
            var time = parseFloat(cmd.value);
            tvVideoPlayer.currentTime = time;
            showRemoteToast('⏩ Seek to ' + formatTime(time));
          }
          break;
        case 'seek_relative':
          if (cmd.value !== null) {
            var delta = parseFloat(cmd.value);
            tvVideoPlayer.currentTime = Math.max(0, tvVideoPlayer.currentTime + delta);
            showRemoteToast((delta > 0 ? '⏩ +' : '⏪ ') + delta + 's');
          }
          break;
        case 'volume':
          if (cmd.value !== null) {
            var vol = parseFloat(cmd.value);
            tvVideoPlayer.volume = Math.max(0, Math.min(1, vol));
            showRemoteToast('🔊 Volume ' + Math.round(tvVideoPlayer.volume * 100) + '%');
          }
          break;
        case 'mute':
          tvVideoPlayer.muted = !tvVideoPlayer.muted;
          showRemoteToast(tvVideoPlayer.muted ? '🔇 Muted' : '🔊 Unmuted');
          break;
        case 'load':
          if (cmd.value) {
            playMediaById(cmd.value);
            showRemoteToast('🎬 Loading movie from Phone');
          }
          break;
        case 'fullscreen':
          toggleFullscreen();
          break;
      }
    }

    function toggleFullscreen() {
      if (!document.fullscreenElement) {
        if (playerModal.requestFullscreen) {
          playerModal.requestFullscreen();
        }
      } else {
        if (document.exitFullscreen) {
          document.exitFullscreen();
        }
      }
    }

    document.addEventListener('keydown', function(e) {
      var isModalOpen = playerModal.classList.contains('active');
      if (isModalOpen) {
        if (e.key === 'Escape' || e.key === 'Back' || e.keyCode === 10009) {
          closePlayer();
          e.preventDefault();
        } else if (e.key === ' ' || e.key === 'Enter') {
          if (tvVideoPlayer.paused) tvVideoPlayer.play(); else tvVideoPlayer.pause();
          e.preventDefault();
        } else if (e.key === 'ArrowRight') {
          tvVideoPlayer.currentTime = Math.min(tvVideoPlayer.duration, tvVideoPlayer.currentTime + 10);
          showRemoteToast('⏩ +10s');
          e.preventDefault();
        } else if (e.key === 'ArrowLeft') {
          tvVideoPlayer.currentTime = Math.max(0, tvVideoPlayer.currentTime - 10);
          showRemoteToast('⏪ -10s');
          e.preventDefault();
        } else if (e.key === 'ArrowUp') {
          tvVideoPlayer.volume = Math.min(1, tvVideoPlayer.volume + 0.1);
          showRemoteToast('🔊 ' + Math.round(tvVideoPlayer.volume * 100) + '%');
          e.preventDefault();
        } else if (e.key === 'ArrowDown') {
          tvVideoPlayer.volume = Math.max(0, tvVideoPlayer.volume - 0.1);
          showRemoteToast('🔊 ' + Math.round(tvVideoPlayer.volume * 100) + '%');
          e.preventDefault();
        } else if (e.key === 'f' || e.key === 'F') {
          toggleFullscreen();
          e.preventDefault();
        }
      } else {
        var cards = document.querySelectorAll('.media-card');
        if (cards.length === 0) return;

        if (e.key === 'ArrowRight' || e.key === 'ArrowDown') {
          focusedIndex = (focusedIndex + 1) % cards.length;
          cards[focusedIndex].focus();
          e.preventDefault();
        } else if (e.key === 'ArrowLeft' || e.key === 'ArrowUp') {
          focusedIndex = (focusedIndex - 1 + cards.length) % cards.length;
          cards[focusedIndex].focus();
          e.preventDefault();
        }
      }
    });

    function triggerUpload() {
      document.getElementById('fileUploadInput').click();
    }

    async function handleFileUpload(event) {
      var file = event.target.files[0];
      if (!file) return;

      showRemoteToast('Uploading ' + file.name + '...');
      try {
        var res = await fetch('/api/upload', {
          method: 'POST',
          body: file,
          headers: {
            'X-Filename': encodeURIComponent(file.name),
            'Content-Type': file.type || 'application/octet-stream'
          }
        });
        if (res.ok) {
          showRemoteToast('Upload complete!');
          loadMedia();
        } else {
          showRemoteToast('Upload failed');
        }
      } catch (e) {
        showRemoteToast('Upload error');
      }
    }

    function formatTime(seconds) {
      if (!seconds || isNaN(seconds)) return '0:00';
      var mins = Math.floor(seconds / 60);
      var secs = Math.floor(seconds % 60);
      return mins + ':' + (secs < 10 ? '0' : '') + secs;
    }

    function escapeHtml(str) {
      if (!str) return '';
      return str.replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;').replace(/"/g, '&quot;');
    }

    loadMedia();
  </script>
</body>
</html>
        """.trimIndent()
    }
}
