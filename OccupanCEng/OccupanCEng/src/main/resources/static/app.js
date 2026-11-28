let allRooms = [];
let myRecentBookings = JSON.parse(localStorage.getItem('engispace_recent_bookings') || '[]');
let searchStatusFilter = 'ALL';
let searchBuildingFilter = 'ALL';

const HIERARCHY = {
  'Old Engineering Building': {
    '1st Floor': ['SMART 101'],
    '2nd Floor': ['SMART 201', 'Rm. 202', 'Rm. 203', 'SMART 204', 'DES LAB'],
    '3rd Floor': ['ELECTRONICS LAB', 'Rm. 302', 'Rm. 303', 'Rm. 304', 'Rm. 305', 'Rm. 306']
  },
  'Engineering Laboratory Building': {
    '1st Floor': ['ELB 11', 'ELB 12', 'ELB 13'],
    '2nd Floor': ['ELB 21', 'ELB 22', 'ELB 23'],
    '3rd Floor': ['ELB 31', 'ELB 32', 'ELB 33'],
    '4th Floor': ['ELB 41', 'ELB 42', 'ELB 43'],
    '5th Floor': ['ELB 51', 'ELB 52', 'ELB 53']
  }
};

const BUILDING_ORDER = [
  'Old Engineering Building',
  'Engineering Laboratory Building'
];

const ROOM_LOOKUP = new Map();
Object.entries(HIERARCHY).forEach(([building, floors]) => {
  Object.entries(floors).forEach(([floor, rooms]) => {
    rooms.forEach((roomName, idx) => {
      ROOM_LOOKUP.set(roomName.toLowerCase(), {
        building,
        floor,
        order: idx
      });
    });
  });
});

const sectionMeta = {
  dashboard: { title: 'Dashboard' },
  classrooms: { title: 'Classrooms' },
  schedule: { title: 'Add Schedule' },
  maintenance: { title: 'Maintenance' }
};

function isOccupiedByYou(room) {
  if (!room || (room.status || '').toUpperCase() !== 'OCCUPIED') return false;
  const bookedNames = new Set(myRecentBookings.map(b => b.room));
  if (bookedNames.has(room.name)) return true;
  const savedUser = (localStorage.getItem('engispace_current_user') || '').trim().toLowerCase();
  return Boolean(savedUser && (room.occupant || '').trim().toLowerCase() === savedUser);
}

// Navigation & Sidebar
const sidebar = document.getElementById('sidebar');
const backdrop = document.getElementById('sidebarBackdrop');

document.getElementById('openSidebarBtn').addEventListener('click', () => {
  sidebar.classList.add('open');
  backdrop.classList.add('show');
});

function closeMobileSidebar() {
  sidebar.classList.remove('open');
  backdrop.classList.remove('show');
}

backdrop.addEventListener('click', closeMobileSidebar);

function navigateTo(sectionKey) {
  const maintPw = document.getElementById('maintPassword');
  if (maintPw) maintPw.value = '';

  document.querySelectorAll('.nav-item').forEach(btn => {
    btn.classList.toggle('active', btn.dataset.section === sectionKey);
  });

  document.querySelectorAll('.section-view').forEach(sec => {
    sec.classList.toggle('active', sec.id === `sec-${sectionKey}`);
  });

  if (sectionMeta[sectionKey]) {
    document.getElementById('pageTitle').textContent = sectionMeta[sectionKey].title;
    document.title = `${sectionMeta[sectionKey].title} — EngiSpace`;
  }

  closeMobileSidebar();
}

function openClassroomsWithFilter(status) {
  searchStatusFilter = status;
  document.querySelectorAll('#searchStatusFilters .pill').forEach(p => {
    p.classList.toggle('active', p.dataset.status === status);
  });
  renderClassroomsDirectory();
  navigateTo('classrooms');
}

document.querySelectorAll('.nav-item').forEach(btn => {
  btn.addEventListener('click', () => navigateTo(btn.dataset.section));
});

// Toast helper
let toastTimer;
function showToast(msg) {
  const toast = document.getElementById('toast');
  toast.textContent = msg;
  toast.classList.add('show');
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => toast.classList.remove('show'), 3200);
}

function normalizeFloorLabel(raw) {
  const s = (raw || '').trim().toLowerCase();
  if (s.startsWith('1')) return '1st Floor';
  if (s.startsWith('2')) return '2nd Floor';
  if (s.startsWith('3')) return '3rd Floor';
  if (s.startsWith('4')) return '4th Floor';
  if (s.startsWith('5')) return '5th Floor';
  return raw ? raw.trim() : '1st Floor';
}

function parseBuildingAndFloor(room) {
  const nameClean = (room.name || '').trim();
  const exact = ROOM_LOOKUP.get(nameClean.toLowerCase());
  if (exact) {
    return exact;
  }

  const rawLoc = (room.location || '').trim();
  const lowerLoc = rawLoc.toLowerCase();
  const upperName = nameClean.toUpperCase();

  let building = 'Old Engineering Building';
  if (upperName.startsWith('ELB') || lowerLoc.includes('elb') || lowerLoc.includes('laboratory')) {
    building = 'Engineering Laboratory Building';
  }

  let floor = '1st Floor';
  const parts = rawLoc.split(',').map(s => s.trim()).filter(Boolean);
  const floorToken = parts.find(p => /^(1st|2nd|3rd|4th|5th)/i.test(p) || /floor/i.test(p));
  if (floorToken) {
    floor = normalizeFloorLabel(floorToken);
  } else {
    const numMatch = nameClean.match(/\b([1-5])\d{1,2}\b/);
    if (numMatch) {
      floor = normalizeFloorLabel(numMatch[1]);
    }
  }

  return { building, floor, order: 999 };
}

function getFloorSortRank(floorLabel) {
  const lower = floorLabel.toLowerCase();
  if (lower.startsWith('1')) return 1;
  if (lower.startsWith('2')) return 2;
  if (lower.startsWith('3')) return 3;
  if (lower.startsWith('4')) return 4;
  if (lower.startsWith('5')) return 5;
  const num = parseInt(lower.replace(/\D/g, ''), 10);
  return isNaN(num) ? 99 : num;
}

// Fetch Data from Spring Boot API
async function refreshAllData() {
  try {
    const roomsRes = await fetch('/api/rooms');
    if (roomsRes.ok) {
      allRooms = await roomsRes.json();
    }
    renderAll();
  } catch (err) {
    console.error('Error loading rooms:', err);
    showToast('Could not load classroom data.');
  }
}

function renderAll() {
  updateStatsAndDashboard();
  renderClassroomsDirectory();
  populateRoomDropdowns();
}

// Dashboard Top Search Bar Logic
const dashInput = document.getElementById('dashboardSearchInput');
const dashDropdown = document.getElementById('dashboardSearchDropdown');

dashInput.addEventListener('input', () => {
  const q = dashInput.value.trim().toLowerCase();
  if (!q) {
    dashDropdown.classList.remove('show');
    dashDropdown.innerHTML = '';
    return;
  }

  const matches = allRooms.filter(r => {
    const { building, floor } = parseBuildingAndFloor(r);
    const haystack = `${r.name || ''} ${building} ${floor} ${r.occupant || ''} ${r.faculty || ''}`.toLowerCase();
    return haystack.includes(q);
  }).slice(0, 8);

  if (matches.length === 0) {
    dashDropdown.innerHTML = `<div class="search-dropdown-empty">No matching classroom found for "${escapeHtml(dashInput.value.trim())}"</div>`;
    dashDropdown.classList.add('show');
    return;
  }

  dashDropdown.innerHTML = matches.map(r => {
    const status = (r.status || 'VACANT').toUpperCase();
    const badgeClass = status === 'OCCUPIED' ? 'badge-occupied' : status === 'MAINTENANCE' ? 'badge-maintenance' : 'badge-vacant';
    const { building, floor } = parseBuildingAndFloor(r);
    return `
      <div class="search-dropdown-item" onclick="selectDashboardSearchResult('${escapeJs(r.name)}')">
        <div>
          <strong style="font-size:0.9rem;">${escapeHtml(r.name)}</strong>
          <div style="font-size:0.76rem;color:var(--text-muted);">${escapeHtml(building)} • ${escapeHtml(floor)}</div>
        </div>
        <span class="badge ${badgeClass}">${escapeHtml(status)}</span>
      </div>
    `;
  }).join('');

  dashDropdown.classList.add('show');
});

dashInput.addEventListener('keydown', (e) => {
  if (e.key === 'Enter') {
    e.preventDefault();
    submitDashboardSearch();
  }
});

document.addEventListener('click', (e) => {
  if (!e.target.closest('.dashboard-search-bar')) {
    dashDropdown.classList.remove('show');
  }
});

function selectDashboardSearchResult(roomName) {
  dashDropdown.classList.remove('show');
  dashInput.value = '';
  openRoomModal(roomName);
}

function submitDashboardSearch() {
  const q = dashInput.value.trim().toLowerCase();
  if (!q) {
    showToast('Enter a room name or occupant to search.');
    return;
  }

  const exact = allRooms.find(r => (r.name || '').toLowerCase() === q);
  if (exact) {
    selectDashboardSearchResult(exact.name);
    return;
  }

  const partial = allRooms.filter(r => {
    const { building, floor } = parseBuildingAndFloor(r);
    const haystack = `${r.name || ''} ${building} ${floor} ${r.occupant || ''} ${r.faculty || ''}`.toLowerCase();
    return haystack.includes(q);
  });

  if (partial.length === 1) {
    selectDashboardSearchResult(partial[0].name);
  } else if (partial.length > 1) {
    dashDropdown.classList.add('show');
  } else {
    showToast(`No classroom found matching "${dashInput.value.trim()}".`);
  }
}

// 1. Dashboard Rendering
function updateStatsAndDashboard() {
  const total = allRooms.length;
  const vacant = allRooms.filter(r => (r.status || '').toUpperCase() === 'VACANT').length;
  const occupied = allRooms.filter(r => (r.status || '').toUpperCase() === 'OCCUPIED').length;
  const maint = allRooms.filter(r => (r.status || '').toUpperCase() === 'MAINTENANCE').length;

  document.getElementById('statTotal').textContent = total;
  document.getElementById('statVacant').textContent = vacant;
  document.getElementById('statOccupied').textContent = occupied;
  document.getElementById('statMaint').textContent = maint;
  document.getElementById('navTotalCount').textContent = total;

  const currentlyOccupiedNames = new Set(
    allRooms
      .filter(r => (r.status || '').toUpperCase() === 'OCCUPIED')
      .map(r => r.name)
  );
  myRecentBookings = myRecentBookings.filter(item => currentlyOccupiedNames.has(item.room));
  localStorage.setItem('engispace_recent_bookings', JSON.stringify(myRecentBookings));

  const recentContainer = document.getElementById('recentOccupiedContainer');
  const occupiedList = allRooms.filter(r => (r.status || '').toUpperCase() === 'OCCUPIED');

  if (occupiedList.length === 0) {
    recentContainer.innerHTML = `
      <div class="empty-state" style="padding:28px 16px;">
        <h3>No Rooms Currently Occupied</h3>
        <p>All active classrooms are vacant. Schedule a room to see it listed here.</p>
        <button class="btn btn-primary" onclick="navigateTo('schedule')">Schedule Room</button>
      </div>
    `;
    return;
  }

  const sortedRecent = [...occupiedList].sort((a, b) => {
    const aMine = isOccupiedByYou(a) ? 0 : 1;
    const bMine = isOccupiedByYou(b) ? 0 : 1;
    return aMine - bMine;
  }).slice(0, 5);

  recentContainer.innerHTML = sortedRecent.map(room => {
    const isMine = isOccupiedByYou(room);
    const { building, floor } = parseBuildingAndFloor(room);
    const vacateBtn = isMine
      ? `<button class="btn btn-danger-soft" onclick="vacateRoom('${escapeJs(room.name)}')">Vacate</button>`
      : `<span style="font-size:0.74rem;color:var(--text-muted);font-weight:600;">Occupied</span>`;

    return `
      <div class="recent-item ${isMine ? 'you-booked' : ''}">
        <div class="recent-meta">
          <h4>
            ${escapeHtml(room.name)}
            ${isMine ? '<span class="tag-you">Occupied by You</span>' : ''}
          </h4>
          <p><strong>${escapeHtml(room.occupant || 'Occupied')}</strong> • ${escapeHtml(building)} (${escapeHtml(floor)})</p>
          <p>${escapeHtml(room.schedule || 'Active schedule')}</p>
        </div>
        ${vacateBtn}
      </div>
    `;
  }).join('');
}

// 2. Classrooms Directory
function renderClassroomsDirectory() {
  const container = document.getElementById('searchResultsContainer');

  const list = allRooms.filter(r => {
    const { building } = parseBuildingAndFloor(r);
    const matchesBuilding = searchBuildingFilter === 'ALL' || building === searchBuildingFilter;
    const matchesStatus = searchStatusFilter === 'ALL' || (r.status || '').toUpperCase() === searchStatusFilter;
    return matchesBuilding && matchesStatus;
  });

  if (list.length === 0) {
    container.innerHTML = `
      <div class="empty-state">
        <h3>No Matching Classrooms Found</h3>
        <p>No rooms matched your current filters. Reset filters to view all buildings and floors.</p>
        <button class="btn btn-secondary" onclick="resetFilters()">Reset Filters</button>
      </div>
    `;
    return;
  }

  const buildingMap = new Map();
  BUILDING_ORDER.forEach(b => buildingMap.set(b, new Map()));

  list.forEach(room => {
    const { building, floor } = parseBuildingAndFloor(room);
    if (!buildingMap.has(building)) {
      buildingMap.set(building, new Map());
    }
    const floorMap = buildingMap.get(building);
    if (!floorMap.has(floor)) {
      floorMap.set(floor, []);
    }
    floorMap.get(floor).push(room);
  });

  let html = '';

  buildingMap.forEach((floorMap, buildingName) => {
    if (floorMap.size === 0) return;

    let buildingTotal = 0;
    let buildingVacant = 0;

    floorMap.forEach(rooms => {
      buildingTotal += rooms.length;
      buildingVacant += rooms.filter(r => (r.status || '').toUpperCase() === 'VACANT').length;
    });

    const sortedFloors = Array.from(floorMap.entries()).sort(
      (a, b) => getFloorSortRank(a[0]) - getFloorSortRank(b[0])
    );

    const floorsHtml = sortedFloors.map(([floorName, rooms]) => {
      const sortedRooms = [...rooms].sort((a, b) => {
        const infoA = parseBuildingAndFloor(a);
        const infoB = parseBuildingAndFloor(b);
        if (infoA.order !== infoB.order) return infoA.order - infoB.order;
        return (a.name || '').localeCompare(b.name || '', undefined, { numeric: true });
      });

      return `
        <div class="floor-block">
          <div class="floor-header">
            <span class="floor-pill">${escapeHtml(floorName)}</span>
            <div class="floor-line"></div>
            <span class="floor-count">${sortedRooms.length} ${sortedRooms.length === 1 ? 'room' : 'rooms'}</span>
          </div>
          <div class="rooms-grid">
            ${sortedRooms.map(r => buildRoomCard(r)).join('')}
          </div>
        </div>
      `;
    }).join('');

    html += `
      <div class="building-section">
        <div class="building-header">
          <div class="building-title-wrap">
            <div class="building-badge-icon">
              <svg width="20" height="20" fill="none" stroke="currentColor" stroke-width="2" viewBox="0 0 24 24">
                <path stroke-linecap="round" stroke-linejoin="round" d="M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5m-4 0h4"/>
              </svg>
            </div>
            <h3>${escapeHtml(buildingName)}</h3>
          </div>
          <div class="building-stats">
            <span class="b-chip">${buildingTotal} Total</span>
            <span class="b-chip vacant">${buildingVacant} Vacant</span>
          </div>
        </div>
        ${floorsHtml}
      </div>
    `;
  });

  container.innerHTML = html;
}

function resetFilters() {
  searchStatusFilter = 'ALL';
  searchBuildingFilter = 'ALL';
  document.querySelectorAll('#searchStatusFilters .pill').forEach(p => {
    p.classList.toggle('active', p.dataset.status === 'ALL');
  });
  document.querySelectorAll('#searchBuildingFilters .pill').forEach(p => {
    p.classList.toggle('active', p.dataset.building === 'ALL');
  });
  renderClassroomsDirectory();
}

document.querySelectorAll('#searchBuildingFilters .pill').forEach(pill => {
  pill.addEventListener('click', () => {
    document.querySelectorAll('#searchBuildingFilters .pill').forEach(p => p.classList.remove('active'));
    pill.classList.add('active');
    searchBuildingFilter = pill.dataset.building;
    renderClassroomsDirectory();
  });
});

document.querySelectorAll('#searchStatusFilters .pill').forEach(pill => {
  pill.addEventListener('click', () => {
    document.querySelectorAll('#searchStatusFilters .pill').forEach(p => p.classList.remove('active'));
    pill.classList.add('active');
    searchStatusFilter = pill.dataset.status;
    renderClassroomsDirectory();
  });
});

// Minimal Card: Only Room Name & Status
function buildRoomCard(room) {
  const status = (room.status || 'VACANT').toUpperCase();
  let badgeClass = 'badge-vacant';
  if (status === 'OCCUPIED') badgeClass = 'badge-occupied';
  if (status === 'MAINTENANCE') badgeClass = 'badge-maintenance';

  return `
    <div class="room-card" onclick="openRoomModal('${escapeJs(room.name)}')" role="button" tabindex="0"
         onkeydown="if(event.key==='Enter'||event.key===' '){event.preventDefault();openRoomModal('${escapeJs(room.name)}');}">
      <span class="room-name">${escapeHtml(room.name)}</span>
      <span class="badge ${badgeClass}">${escapeHtml(status)}</span>
    </div>
  `;
}

// Modal Shown When Clicking a Room Card
function openRoomModal(roomName) {
  const room = allRooms.find(r => r.name === roomName);
  if (!room) return;

  const { building, floor } = parseBuildingAndFloor(room);
  const status = (room.status || 'VACANT').toUpperCase();
  const isMine = isOccupiedByYou(room);

  document.getElementById('modalRoomName').textContent = room.name;
  document.getElementById('modalRoomLoc').textContent = `${building} • ${floor}`;

  const badge = document.getElementById('modalRoomBadge');
  badge.textContent = status;
  badge.className = 'badge ' + (
    status === 'OCCUPIED' ? 'badge-occupied' :
    status === 'MAINTENANCE' ? 'badge-maintenance' : 'badge-vacant'
  );

  const detailsEl = document.getElementById('modalRoomDetails');
  const actionEl = document.getElementById('modalPrimaryAction');

  if (status === 'VACANT') {
    detailsEl.innerHTML = `<p style="color:var(--vacant-text);font-weight:500;">This classroom is currently vacant and ready for scheduling.</p>`;
    actionEl.innerHTML = `<button type="button" class="btn btn-primary" onclick="closeRoomModal(); quickBookRoom('${escapeJs(room.name)}')">Schedule This Room</button>`;
  } else if (status === 'OCCUPIED') {
    detailsEl.innerHTML = `
      <div><strong>Occupant:</strong> ${escapeHtml(room.occupant || 'N/A')}${isMine ? ' <span class="tag-you">You</span>' : ''}</div>
      ${room.faculty ? `<div><strong>Faculty:</strong> ${escapeHtml(room.faculty)}</div>` : ''}
      ${room.reason ? `<div><strong>Purpose:</strong> ${escapeHtml(room.reason)}</div>` : ''}
      <div><strong>Schedule:</strong> ${escapeHtml(room.schedule || 'In session')}</div>
      ${!isMine ? `<p style="margin-top:8px;font-size:0.8rem;color:var(--text-muted);">Only the occupant who booked this room can vacate it.</p>` : ''}
    `;
    actionEl.innerHTML = isMine
      ? `<button type="button" class="btn btn-danger-soft" style="padding:10px 16px;font-size:0.875rem;" onclick="closeRoomModal(); vacateRoom('${escapeJs(room.name)}')">Vacate Room</button>`
      : ``;
  } else {
    detailsEl.innerHTML = `<p style="color:var(--maint-text);font-weight:500;">This classroom is currently under maintenance and cannot be scheduled.</p>`;
    actionEl.innerHTML = ``;
  }

  document.getElementById('roomActionModal').classList.add('show');
}

function closeRoomModal(event) {
  if (event && event.target !== event.currentTarget) return;
  document.getElementById('roomActionModal').classList.remove('show');
}

document.addEventListener('keydown', (e) => {
  if (e.key === 'Escape') closeRoomModal();
});

// 3. Add Schedule
function populateRoomDropdowns() {
  const schedSelect = document.getElementById('schedRoom');
  const maintSelect = document.getElementById('maintRoom');
  const currentSchedVal = schedSelect.value;
  const currentMaintVal = maintSelect.value;

  const sortByHierarchy = (a, b) => {
    const infoA = parseBuildingAndFloor(a);
    const infoB = parseBuildingAndFloor(b);
    const bRankA = BUILDING_ORDER.indexOf(infoA.building);
    const bRankB = BUILDING_ORDER.indexOf(infoB.building);
    if (bRankA !== bRankB) return bRankA - bRankB;
    const fRankA = getFloorSortRank(infoA.floor);
    const fRankB = getFloorSortRank(infoB.floor);
    if (fRankA !== fRankB) return fRankA - fRankB;
    return infoA.order - infoB.order;
  };

  const vacantRooms = allRooms
    .filter(r => (r.status || '').toUpperCase() === 'VACANT')
    .sort(sortByHierarchy);

  const sortedAll = [...allRooms].sort(sortByHierarchy);

  schedSelect.innerHTML = `<option value="">-- Choose a vacant room (${vacantRooms.length} available) --</option>` +
    vacantRooms.map(r => {
      const { building, floor } = parseBuildingAndFloor(r);
      return `<option value="${escapeHtml(r.name)}">${escapeHtml(r.name)} (${escapeHtml(building)} — ${escapeHtml(floor)})</option>`;
    }).join('');

  maintSelect.innerHTML = `<option value="">-- Select classroom --</option>` +
    sortedAll.map(r => {
      const { building, floor } = parseBuildingAndFloor(r);
      return `<option value="${escapeHtml(r.name)}">${escapeHtml(r.name)} (${escapeHtml(building)}, ${escapeHtml(floor)}) [${escapeHtml(r.status)}]</option>`;
    }).join('');

  if (currentSchedVal) schedSelect.value = currentSchedVal;
  if (currentMaintVal) maintSelect.value = currentMaintVal;
}

function quickBookRoom(roomName) {
  navigateTo('schedule');
  document.getElementById('schedRoom').value = roomName;
  document.getElementById('schedOccupant').focus();
}

function clearFormErrors() {
  document.querySelectorAll('#scheduleForm .form-group').forEach(fg => fg.classList.remove('has-error'));
  const banner = document.getElementById('scheduleAlert');
  banner.className = 'alert-banner';
  banner.textContent = '';
}

document.getElementById('scheduleForm').addEventListener('submit', async (e) => {
  e.preventDefault();
  clearFormErrors();

  const room = document.getElementById('schedRoom').value.trim();
  const occupant = document.getElementById('schedOccupant').value.trim();
  const faculty = document.getElementById('schedFaculty').value.trim();
  const reason = document.getElementById('schedReason').value.trim();
  const start = document.getElementById('schedStart').value;
  const end = document.getElementById('schedEnd').value;

  let valid = true;
  if (!room) { document.getElementById('fg-room').classList.add('has-error'); valid = false; }
  if (!occupant) { document.getElementById('fg-occupant').classList.add('has-error'); valid = false; }
  if (!faculty) { document.getElementById('fg-faculty').classList.add('has-error'); valid = false; }
  if (!reason) { document.getElementById('fg-reason').classList.add('has-error'); valid = false; }
  if (!start) { document.getElementById('fg-start').classList.add('has-error'); valid = false; }
  if (!end || (start && new Date(end) <= new Date(start))) {
    document.getElementById('fg-end').classList.add('has-error');
    valid = false;
  }

  if (!valid) return;

  const btn = document.getElementById('submitScheduleBtn');
  btn.disabled = true;
  btn.textContent = 'Scheduling...';

  try {
    const res = await fetch('/api/schedule', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ room, occupant, faculty, reason, start, end })
    });

    const msg = await res.text();
    const banner = document.getElementById('scheduleAlert');

    if (msg === 'Success') {
      localStorage.setItem('engispace_current_user', occupant);
      myRecentBookings.unshift({ room, occupant, faculty, reason, start, end });
      localStorage.setItem('engispace_recent_bookings', JSON.stringify(myRecentBookings.slice(0, 10)));

      banner.className = 'alert-banner alert-success show';
      banner.innerHTML = `<span><strong>Success!</strong> ${escapeHtml(room)} is now occupied by ${escapeHtml(occupant)}.</span>
                          <button type="button" class="btn btn-secondary" style="padding:5px 10px;font-size:0.78rem;" onclick="navigateTo('dashboard')">View Dashboard</button>`;
      showToast(`${room} scheduled successfully!`);
      document.getElementById('scheduleForm').reset();
      setDefaultDateTimes();
      await refreshAllData();
    } else {
      banner.className = 'alert-banner alert-error show';
      banner.textContent = msg || 'Unable to schedule room.';
    }
  } catch (err) {
    showToast('Network error while scheduling room.');
  } finally {
    btn.disabled = false;
    btn.textContent = 'Confirm & Occupy Room';
  }
});

async function vacateRoom(roomName) {
  const targetRoom = allRooms.find(r => r.name === roomName);
  if (!targetRoom || !isOccupiedByYou(targetRoom)) {
    showToast('You can only vacate rooms that you occupied.');
    return;
  }

  try {
    const res = await fetch(`/api/leave/${encodeURIComponent(roomName)}`, { method: 'POST' });
    const msg = await res.text();
    myRecentBookings = myRecentBookings.filter(item => item.room !== roomName);
    localStorage.setItem('engispace_recent_bookings', JSON.stringify(myRecentBookings));
    showToast(msg || `${roomName} vacated.`);
    await refreshAllData();
  } catch (e) {
    showToast('Failed to vacate room.');
  }
}

// 4. Maintenance
async function handleMaintenance(action) {
  const room = document.getElementById('maintRoom').value;
  const passwordInput = document.getElementById('maintPassword');
  const password = passwordInput.value;
  const alertBox = document.getElementById('maintAlert');

  passwordInput.value = '';
  passwordInput.blur();

  if (!room || !password) {
    alertBox.className = 'alert-banner alert-error show';
    alertBox.textContent = 'Please select a classroom and enter the admin password.';
    return;
  }

  const endpoint = action === 'set' ? '/api/maintenance' : '/api/maintenance/remove';

  try {
    const res = await fetch(endpoint, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({ room, password })
    });
    const msg = await res.text();
    const isOk = !msg.toLowerCase().includes('invalid') && !msg.toLowerCase().includes('not found');

    alertBox.className = `alert-banner ${isOk ? 'alert-success' : 'alert-error'} show`;
    alertBox.textContent = msg;
    showToast(msg);

    if (isOk) {
      await refreshAllData();
    }
  } catch (e) {
    showToast('Error updating maintenance status.');
  }
}

// Helpers
function escapeHtml(str) {
  if (str === null || str === undefined) return '';
  return String(str)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
    .replace(/'/g, '&#39;');
}

function escapeJs(str) {
  return String(str || '').replace(/\\/g, '\\\\').replace(/'/g, "\\'");
}

function setDefaultDateTimes() {
  const now = new Date();
  now.setMinutes(now.getMinutes() - now.getTimezoneOffset());
  const later = new Date(now.getTime() + 60 * 60 * 1000);
  document.getElementById('schedStart').value = now.toISOString().slice(0, 16);
  document.getElementById('schedEnd').value = later.toISOString().slice(0, 16);
}

setDefaultDateTimes();
refreshAllData();