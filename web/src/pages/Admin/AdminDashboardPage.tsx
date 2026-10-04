import React, { useEffect, useState, useCallback } from 'react';
import {
  Users,
  Shield,
  ShieldCheck,
  ShieldAlert,
  Smartphone,
  CheckCircle,
  XCircle,
  AlertTriangle,
  RefreshCw,
  Search,
  Filter,
  Eye,
  Edit2,
  UserCheck,
  UserX,
  History,
  Lock,
  ChevronLeft,
  ChevronRight,
  X,
  Save,
  Activity,
  Trash2
} from 'lucide-react';
import { adminService } from '../../services/adminService';
import {
  AdminStats,
  AdminUserSummary,
  AdminUserDetail,
  AdminAuditLog,
  AdminUserUpdatePayload,
  PaginatedData,
  UserStatus
} from '../../types/admin';
import { RoleType } from '../../types/auth';
import { authService } from '../../services/authService';

export const AdminDashboardPage: React.FC = () => {
  // Navigation / Tab state
  const [activeTab, setActiveTab] = useState<'overview' | 'users' | 'security'>('overview');

  // Stats state
  const [stats, setStats] = useState<AdminStats | null>(null);
  const [loadingStats, setLoadingStats] = useState<boolean>(true);

  // Users table state
  const [usersData, setUsersData] = useState<PaginatedData<AdminUserSummary> | null>(null);
  const [loadingUsers, setLoadingUsers] = useState<boolean>(false);
  const [searchQuery, setSearchQuery] = useState<string>('');
  const [roleFilter, setRoleFilter] = useState<RoleType | ''>('');
  const [statusFilter, setStatusFilter] = useState<UserStatus | ''>('');
  const [page, setPage] = useState<number>(0);
  const pageSize = 10;

  // Selected User for details
  const [selectedUser, setSelectedUser] = useState<AdminUserDetail | null>(null);
  const [loadingDetails, setLoadingDetails] = useState<boolean>(false);

  // User Edit Modal state
  const [editingUser, setEditingUser] = useState<AdminUserSummary | null>(null);
  const [editFormData, setEditFormData] = useState<AdminUserUpdatePayload>({ name: '', email: '', phone: '', status: 'ACTIVE' });
  const [editError, setEditError] = useState<string | null>(null);
  const [editSaving, setEditSaving] = useState<boolean>(false);

  // Role Change Modal state
  const [roleChangeUser, setRoleChangeUser] = useState<AdminUserSummary | null>(null);
  const [selectedNewRole, setSelectedNewRole] = useState<RoleType>('PARENT');
  const [roleSaving, setRoleSaving] = useState<boolean>(false);
  const [roleError, setRoleError] = useState<string | null>(null);

  // Status Change Confirmation state
  const [statusChangeUser, setStatusChangeUser] = useState<AdminUserSummary | null>(null);
  const [targetNewStatus, setTargetNewStatus] = useState<UserStatus>('DISABLED');
  const [statusSaving, setStatusSaving] = useState<boolean>(false);
  const [statusError, setStatusError] = useState<string | null>(null);

  // Permanent Delete Modal state
  const [deleteConfirmUser, setDeleteConfirmUser] = useState<AdminUserSummary | null>(null);
  const [deleteSaving, setDeleteSaving] = useState<boolean>(false);
  const [deleteError, setDeleteError] = useState<string | null>(null);

  // Audit Logs state
  const [auditLogsData, setAuditLogsData] = useState<PaginatedData<AdminAuditLog> | null>(null);
  const [loadingAudit, setLoadingAudit] = useState<boolean>(false);
  const [auditPage, setAuditPage] = useState<number>(0);

  // Global notice
  const [globalMessage, setGlobalMessage] = useState<{ text: string; type: 'success' | 'error' } | null>(null);

  const currentAdmin = authService.getCurrentUser();

  const showNotice = (text: string, type: 'success' | 'error' = 'success') => {
    setGlobalMessage({ text, type });
    setTimeout(() => {
      setGlobalMessage(null);
    }, 4000);
  };

  // Fetch Stats
  const loadStats = useCallback(async () => {
    setLoadingStats(true);
    try {
      const data = await adminService.getStats();
      setStats(data);
    } catch (err: any) {
      console.error('Failed to load admin stats:', err);
    } finally {
      setLoadingStats(false);
    }
  }, []);

  // Fetch Users
  const loadUsers = useCallback(async () => {
    setLoadingUsers(true);
    try {
      const data = await adminService.getUsers({
        page,
        size: pageSize,
        search: searchQuery,
        role: roleFilter,
        status: statusFilter
      });
      setUsersData(data);
    } catch (err: any) {
      console.error('Failed to load users:', err);
      showNotice(err.response?.data?.message || 'Failed to load users', 'error');
    } finally {
      setLoadingUsers(false);
    }
  }, [page, searchQuery, roleFilter, statusFilter]);

  // Fetch Audit Logs
  const loadAuditLogs = useCallback(async () => {
    setLoadingAudit(true);
    try {
      const data = await adminService.getAuditLogs({ page: auditPage, size: 15 });
      setAuditLogsData(data);
    } catch (err: any) {
      console.error('Failed to load audit logs:', err);
    } finally {
      setLoadingAudit(false);
    }
  }, [auditPage]);

  useEffect(() => {
    loadStats();
  }, [loadStats]);

  useEffect(() => {
    loadUsers();
  }, [loadUsers]);

  useEffect(() => {
    if (activeTab === 'security') {
      loadAuditLogs();
    }
  }, [activeTab, loadAuditLogs]);

  // Handle Search Input
  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(0);
    loadUsers();
  };

  // View User Details
  const handleViewUser = async (userId: number) => {
    setLoadingDetails(true);
    try {
      const detail = await adminService.getUser(userId);
      setSelectedUser(detail);
    } catch (err: any) {
      showNotice(err.response?.data?.message || 'Failed to fetch user profile', 'error');
    } finally {
      setLoadingDetails(false);
    }
  };

  // Start Edit
  const handleStartEdit = (user: AdminUserSummary) => {
    setEditingUser(user);
    setEditFormData({
      name: user.name,
      email: user.email,
      phone: '',
      status: user.status
    });
    setEditError(null);
  };

  // Save Edit
  const handleSaveEdit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!editingUser) return;
    if (!editFormData.name.trim()) {
      setEditError('Name is required');
      return;
    }
    if (!editFormData.email.trim()) {
      setEditError('Email is required');
      return;
    }

    setEditSaving(true);
    setEditError(null);
    try {
      await adminService.updateUser(editingUser.id, editFormData);
      showNotice(`User ${editFormData.email} updated successfully`, 'success');
      setEditingUser(null);
      loadUsers();
      loadStats();
      if (selectedUser?.id === editingUser.id) {
        handleViewUser(editingUser.id);
      }
    } catch (err: any) {
      setEditError(err.response?.data?.message || 'Failed to update user');
    } finally {
      setEditSaving(false);
    }
  };

  // Start Role Change
  const handleStartRoleChange = (user: AdminUserSummary) => {
    setRoleChangeUser(user);
    setSelectedNewRole(user.role);
    setRoleError(null);
  };

  // Confirm Role Change
  const handleConfirmRoleChange = async () => {
    if (!roleChangeUser) return;
    setRoleSaving(true);
    setRoleError(null);
    try {
      await adminService.updateRole(roleChangeUser.id, selectedNewRole);
      showNotice(`Role for ${roleChangeUser.email} changed to ${selectedNewRole}`, 'success');
      setRoleChangeUser(null);
      loadUsers();
      loadStats();
    } catch (err: any) {
      setRoleError(err.response?.data?.message || 'Failed to update user role');
    } finally {
      setRoleSaving(false);
    }
  };

  // Start Status Change
  const handleStartStatusChange = (user: AdminUserSummary, newStatus: UserStatus) => {
    setStatusChangeUser(user);
    setTargetNewStatus(newStatus);
    setStatusError(null);
  };

  // Confirm Status Change
  const handleConfirmStatusChange = async () => {
    if (!statusChangeUser) return;
    setStatusSaving(true);
    setStatusError(null);
    try {
      await adminService.updateStatus(statusChangeUser.id, targetNewStatus);
      showNotice(`Account ${statusChangeUser.email} status changed to ${targetNewStatus}`, 'success');
      setStatusChangeUser(null);
      loadUsers();
      loadStats();
    } catch (err: any) {
      setStatusError(err.response?.data?.message || 'Failed to update account status');
    } finally {
      setStatusSaving(false);
    }
  };

  // Start Permanent Deletion
  const handleStartDelete = (user: AdminUserSummary) => {
    setDeleteConfirmUser(user);
    setDeleteError(null);
  };

  // Confirm Permanent Deletion
  const handleConfirmDelete = async () => {
    if (!deleteConfirmUser) return;
    setDeleteSaving(true);
    setDeleteError(null);
    try {
      await adminService.deleteUser(deleteConfirmUser.id);
      showNotice(`Account ${deleteConfirmUser.email} permanently deleted`, 'success');
      if (selectedUser?.id === deleteConfirmUser.id) {
        setSelectedUser(null);
      }
      setDeleteConfirmUser(null);
      loadUsers();
      loadStats();
      loadAuditLogs();
    } catch (err: any) {
      setDeleteError(err.response?.data?.message || 'Failed to permanently delete user');
    } finally {
      setDeleteSaving(false);
    }
  };

  const formatDate = (isoString?: string | null) => {
    if (!isoString) return 'Never';
    try {
      return new Date(isoString).toLocaleString('en-US', {
        month: 'short',
        day: 'numeric',
        year: 'numeric',
        hour: '2-digit',
        minute: '2-digit'
      });
    } catch {
      return isoString;
    }
  };

  return (
    <div style={{ maxWidth: '1400px', margin: '0 auto', display: 'flex', flexDirection: 'column', gap: '1.5rem' }}>
      {/* Global Message Banner */}
      {globalMessage && (
        <div
          className={`badge ${globalMessage.type === 'success' ? 'badge-success' : 'badge-danger'}`}
          style={{
            padding: '0.85rem 1.25rem',
            fontSize: '0.9rem',
            borderRadius: 'var(--radius-md)',
            display: 'flex',
            alignItems: 'center',
            gap: '0.5rem',
            boxShadow: '0 4px 12px rgba(0,0,0,0.3)',
          }}
        >
          {globalMessage.type === 'success' ? <CheckCircle size={18} /> : <AlertTriangle size={18} />}
          <span>{globalMessage.text}</span>
        </div>
      )}

      {/* Admin Dashboard Header & Tab Navigation */}
      <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem', borderBottom: '1px solid var(--border-subtle)', paddingBottom: '1rem' }}>
        <div>
          <h1 style={{ fontSize: '1.75rem', fontWeight: 700, color: '#fff', display: 'flex', alignItems: 'center', gap: '0.65rem' }}>
            <ShieldCheck size={28} color="var(--primary)" />
            Administrator Control Center
          </h1>
          <p style={{ color: 'var(--text-muted)', fontSize: '0.875rem', marginTop: '0.25rem' }}>
            Monitor family accounts, enforce system roles, manage device health, and review immutable security audit logs.
          </p>
        </div>

        {/* Tab Buttons */}
        <div style={{ display: 'flex', gap: '0.5rem' }}>
          <button
            type="button"
            id="tab-overview"
            className={`btn btn-sm ${activeTab === 'overview' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('overview')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <Activity size={15} />
            <span>Overview</span>
          </button>
          <button
            type="button"
            id="tab-users"
            className={`btn btn-sm ${activeTab === 'users' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('users')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <Users size={15} />
            <span>Users Directory</span>
          </button>
          <button
            type="button"
            id="tab-security"
            className={`btn btn-sm ${activeTab === 'security' ? 'btn-primary' : 'btn-secondary'}`}
            onClick={() => setActiveTab('security')}
            style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
          >
            <History size={15} />
            <span>Audit &amp; Security</span>
          </button>
          <button
            type="button"
            id="btn-refresh-all"
            className="btn btn-secondary btn-sm"
            onClick={() => {
              loadStats();
              loadUsers();
              if (activeTab === 'security') loadAuditLogs();
            }}
            title="Refresh current view"
          >
            <RefreshCw size={15} className={loadingStats || loadingUsers || loadingAudit ? 'spin' : ''} />
          </button>
        </div>
      </div>

      {/* SECTION 1: SYSTEM OVERVIEW STATS CARDS */}
      <section aria-label="System Metrics Overview">
        <div style={{ display: 'grid', gridTemplateColumns: 'repeat(auto-fit, minmax(200px, 1fr))', gap: '1rem' }}>
          {/* Total Users */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Total Users</span>
              <Users size={18} color="var(--primary)" />
            </div>
            <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#fff' }} id="stat-total-users">
              {loadingStats ? '...' : stats?.totalUsers ?? 0}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>Registered accounts</small>
          </div>

          {/* Parents */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Parents</span>
              <Shield size={18} color="#6366f1" />
            </div>
            <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#6366f1' }} id="stat-total-parents">
              {loadingStats ? '...' : stats?.totalParents ?? 0}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>Parent supervisor accounts</small>
          </div>

          {/* Children */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Children</span>
              <Smartphone size={18} color="#06b6d4" />
            </div>
            <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#06b6d4' }} id="stat-total-children">
              {loadingStats ? '...' : stats?.totalChildren ?? 0}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>Connected child devices</small>
          </div>

          {/* Active Users */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Active Status</span>
              <CheckCircle size={18} color="#10b981" />
            </div>
            <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#10b981' }} id="stat-active-users">
              {loadingStats ? '...' : stats?.activeUsers ?? 0}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>Unrestricted accounts</small>
          </div>

          {/* Disabled Users */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Disabled</span>
              <XCircle size={18} color="#ef4444" />
            </div>
            <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#ef4444' }} id="stat-disabled-users">
              {loadingStats ? '...' : stats?.disabledUsers ?? 0}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>Deactivated accounts</small>
          </div>

          {/* Online Devices */}
          <div className="card" style={{ padding: '1.25rem', display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', color: 'var(--text-muted)' }}>
              <span style={{ fontSize: '0.8rem', fontWeight: 600, textTransform: 'uppercase', letterSpacing: '0.05em' }}>Online Devices</span>
              <Activity size={18} color="#f59e0b" />
            </div>
            <div style={{ fontSize: '1.8rem', fontWeight: 700, color: '#f59e0b' }} id="stat-online-devices">
              {loadingStats ? '...' : stats?.onlineDevices ?? 0}
            </div>
            <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>
              Active within 10 min ({stats?.totalDevices ?? 0} enrolled)
            </small>
          </div>
        </div>
      </section>

      {/* SECTION 2: USERS DIRECTORY & MANAGEMENT TABLE */}
      {(activeTab === 'overview' || activeTab === 'users') && (
        <section className="card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          {/* Header & Filter Controls */}
          <div style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', justifyContent: 'space-between', gap: '1rem' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <Users size={20} color="var(--primary)" />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#fff' }}>User Directory</h2>
              <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
                {usersData?.totalElements ?? 0} Total
              </span>
            </div>

            {/* Search and Filters */}
            <form onSubmit={handleSearchSubmit} style={{ display: 'flex', flexWrap: 'wrap', alignItems: 'center', gap: '0.75rem' }}>
              <div style={{ position: 'relative' }}>
                <Search size={15} style={{ position: 'absolute', left: '10px', top: '50%', transform: 'translateY(-50%)', color: 'var(--text-dim)' }} />
                <input
                  type="text"
                  id="input-admin-search"
                  className="input-field"
                  placeholder="Search name or email..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  style={{ paddingLeft: '2.1rem', width: '220px', height: '36px', fontSize: '0.85rem' }}
                />
              </div>

              {/* Role Filter */}
              <select
                id="select-role-filter"
                className="input-field"
                value={roleFilter}
                onChange={(e) => {
                  setRoleFilter(e.target.value as RoleType | '');
                  setPage(0);
                }}
                style={{ width: '130px', height: '36px', fontSize: '0.85rem' }}
              >
                <option value="">All Roles</option>
                <option value="PARENT">Parent</option>
                <option value="CHILD">Child</option>
                <option value="ADMIN">Admin</option>
              </select>

              {/* Status Filter */}
              <select
                id="select-status-filter"
                className="input-field"
                value={statusFilter}
                onChange={(e) => {
                  setStatusFilter(e.target.value as UserStatus | '');
                  setPage(0);
                }}
                style={{ width: '130px', height: '36px', fontSize: '0.85rem' }}
              >
                <option value="">All Statuses</option>
                <option value="ACTIVE">Active</option>
                <option value="DISABLED">Disabled</option>
                <option value="SUSPENDED">Suspended</option>
                <option value="PENDING">Pending</option>
              </select>

              <button type="submit" id="btn-admin-search-submit" className="btn btn-primary btn-sm" style={{ height: '36px' }}>
                Search
              </button>
            </form>
          </div>

          {/* Table Container */}
          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-dim)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>User</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Role</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Status</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Devices</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Created</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Last Login</th>
                  <th style={{ padding: '0.75rem 1rem', textAlign: 'right' }}>Actions</th>
                </tr>
              </thead>
              <tbody>
                {loadingUsers ? (
                  <tr>
                    <td colSpan={7} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-dim)' }}>
                      <RefreshCw size={24} className="spin" style={{ margin: '0 auto 0.5rem auto' }} />
                      <div>Loading users directory...</div>
                    </td>
                  </tr>
                ) : !usersData || usersData.content.length === 0 ? (
                  <tr>
                    <td colSpan={7} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-dim)' }}>
                      <Users size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.5 }} />
                      <div>No users found matching current filters.</div>
                    </td>
                  </tr>
                ) : (
                  usersData.content.map((user) => {
                    const isSelf = currentAdmin?.id === user.id;

                    return (
                      <tr
                        key={user.id}
                        id={`user-row-${user.id}`}
                        style={{
                          borderBottom: '1px solid var(--border-subtle)',
                          transition: 'background var(--transition-fast)',
                        }}
                      >
                        {/* Name & Email */}
                        <td style={{ padding: '0.85rem 1rem' }}>
                          <div style={{ fontWeight: 600, color: '#fff' }}>{user.name}</div>
                          <small style={{ color: 'var(--text-muted)' }}>{user.email}</small>
                        </td>

                        {/* Role Badge */}
                        <td style={{ padding: '0.85rem 1rem' }}>
                          <span
                            className={`badge ${
                              user.role === 'ADMIN'
                                ? 'badge-primary'
                                : user.role === 'PARENT'
                                ? 'badge-neutral'
                                : 'badge-info'
                            }`}
                            style={{ fontSize: '0.72rem', padding: '0.15rem 0.5rem' }}
                          >
                            {user.role}
                          </span>
                        </td>

                        {/* Status Badge */}
                        <td style={{ padding: '0.85rem 1rem' }}>
                          <span
                            className={`badge ${
                              user.status === 'ACTIVE'
                                ? 'badge-success'
                                : user.status === 'DISABLED'
                                ? 'badge-danger'
                                : 'badge-warning'
                            }`}
                            style={{ fontSize: '0.72rem', padding: '0.15rem 0.5rem' }}
                          >
                            {user.status}
                          </span>
                        </td>

                        {/* Devices */}
                        <td style={{ padding: '0.85rem 1rem' }}>
                          <span style={{ color: user.deviceCount > 0 ? 'var(--text-main)' : 'var(--text-dim)' }}>
                            {user.deviceCount} enrolled
                          </span>
                        </td>

                        {/* Created Date */}
                        <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                          {formatDate(user.createdAt)}
                        </td>

                        {/* Last Login */}
                        <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem' }}>
                          {formatDate(user.lastLoginAt)}
                        </td>

                        {/* Actions */}
                        <td style={{ padding: '0.85rem 1rem', textAlign: 'right' }}>
                          <div style={{ display: 'inline-flex', alignItems: 'center', gap: '0.35rem' }}>
                            {/* View Profile */}
                            <button
                              type="button"
                              id={`btn-view-user-${user.id}`}
                              className="btn btn-secondary btn-sm"
                              title="View full profile"
                              style={{ padding: '0.35rem 0.55rem' }}
                              onClick={() => handleViewUser(user.id)}
                            >
                              <Eye size={14} />
                            </button>

                            {/* Edit Identity */}
                            <button
                              type="button"
                              id={`btn-edit-user-${user.id}`}
                              className="btn btn-secondary btn-sm"
                              title="Edit user details"
                              style={{ padding: '0.35rem 0.55rem' }}
                              onClick={() => handleStartEdit(user)}
                            >
                              <Edit2 size={14} />
                            </button>

                            {/* Change Role (disabled for self) */}
                            <button
                              type="button"
                              id={`btn-role-user-${user.id}`}
                              className="btn btn-secondary btn-sm"
                              title={isSelf ? 'Cannot modify own role' : 'Change user role'}
                              disabled={isSelf}
                              style={{ padding: '0.35rem 0.55rem', opacity: isSelf ? 0.4 : 1 }}
                              onClick={() => handleStartRoleChange(user)}
                            >
                              <Shield size={14} />
                            </button>

                            {/* Toggle Status (Disable / Activate) */}
                            {user.status === 'ACTIVE' ? (
                              <button
                                type="button"
                                id={`btn-disable-user-${user.id}`}
                                className="btn btn-secondary btn-sm"
                                title={isSelf ? 'Cannot disable own account' : 'Disable account'}
                                disabled={isSelf}
                                style={{ padding: '0.35rem 0.55rem', color: isSelf ? 'var(--text-dim)' : 'var(--danger)', opacity: isSelf ? 0.4 : 1 }}
                                onClick={() => handleStartStatusChange(user, 'DISABLED')}
                              >
                                <UserX size={14} />
                              </button>
                            ) : (
                              <button
                                type="button"
                                id={`btn-activate-user-${user.id}`}
                                className="btn btn-secondary btn-sm"
                                title="Reactivate account"
                                style={{ padding: '0.35rem 0.55rem', color: '#10b981' }}
                                onClick={() => handleStartStatusChange(user, 'ACTIVE')}
                              >
                                <UserCheck size={14} />
                              </button>
                            )}

                            {/* Permanent Delete Action */}
                            <button
                              type="button"
                              id={`btn-delete-user-${user.id}`}
                              className="btn btn-secondary btn-sm"
                              title={isSelf ? 'Cannot delete own account' : 'Permanently delete account'}
                              disabled={isSelf}
                              style={{ padding: '0.35rem 0.55rem', color: isSelf ? 'var(--text-dim)' : '#ef4444', opacity: isSelf ? 0.4 : 1 }}
                              onClick={() => handleStartDelete(user)}
                            >
                              <Trash2 size={14} />
                            </button>
                          </div>
                        </td>
                      </tr>
                    );
                  })
                )}
              </tbody>
            </table>
          </div>

          {/* Pagination Controls */}
          {usersData && usersData.totalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-subtle)', paddingTop: '1rem' }}>
              <div style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                Showing page {usersData.number + 1} of {usersData.totalPages} ({usersData.totalElements} users)
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  type="button"
                  id="btn-prev-page"
                  className="btn btn-secondary btn-sm"
                  disabled={usersData.first || loadingUsers}
                  onClick={() => setPage((p) => Math.max(0, p - 1))}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                >
                  <ChevronLeft size={15} />
                  <span>Previous</span>
                </button>
                <button
                  type="button"
                  id="btn-next-page"
                  className="btn btn-secondary btn-sm"
                  disabled={usersData.last || loadingUsers}
                  onClick={() => setPage((p) => p + 1)}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.25rem' }}
                >
                  <span>Next</span>
                  <ChevronRight size={15} />
                </button>
              </div>
            </div>
          )}
        </section>
      )}

      {/* SECTION 3: IMMUTABLE AUDIT LOGS TABLE */}
      {activeTab === 'security' && (
        <section className="card" style={{ padding: '1.5rem', display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
          <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem' }}>
              <History size={20} color="var(--primary)" />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 600, color: '#fff' }}>Administrative Audit Trail</h2>
            </div>
            <span className="badge badge-neutral" style={{ fontSize: '0.75rem' }}>
              {auditLogsData?.totalElements ?? 0} Recorded Actions
            </span>
          </div>

          <div style={{ overflowX: 'auto' }}>
            <table style={{ width: '100%', borderCollapse: 'collapse', textAlign: 'left', fontSize: '0.875rem' }}>
              <thead>
                <tr style={{ borderBottom: '1px solid var(--border-subtle)', color: 'var(--text-dim)', fontSize: '0.75rem', textTransform: 'uppercase', letterSpacing: '0.05em' }}>
                  <th style={{ padding: '0.75rem 1rem' }}>Timestamp</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Actor (ID)</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Target User</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Action</th>
                  <th style={{ padding: '0.75rem 1rem' }}>Details</th>
                  <th style={{ padding: '0.75rem 1rem' }}>IP Address</th>
                </tr>
              </thead>
              <tbody>
                {loadingAudit ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-dim)' }}>
                      <RefreshCw size={24} className="spin" style={{ margin: '0 auto 0.5rem auto' }} />
                      <div>Loading audit logs...</div>
                    </td>
                  </tr>
                ) : !auditLogsData || auditLogsData.content.length === 0 ? (
                  <tr>
                    <td colSpan={6} style={{ padding: '3rem', textAlign: 'center', color: 'var(--text-dim)' }}>
                      <History size={32} style={{ margin: '0 auto 0.5rem auto', opacity: 0.5 }} />
                      <div>No administrative audit logs recorded yet.</div>
                    </td>
                  </tr>
                ) : (
                  auditLogsData.content.map((log) => (
                    <tr key={log.id} style={{ borderBottom: '1px solid var(--border-subtle)' }}>
                      <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)', fontSize: '0.8rem', whiteSpace: 'nowrap' }}>
                        {formatDate(log.timestamp)}
                      </td>
                      <td style={{ padding: '0.85rem 1rem', color: '#fff', fontWeight: 500 }}>
                        {log.userId ? `User #${log.userId}` : 'SYSTEM'}
                      </td>
                      <td style={{ padding: '0.85rem 1rem', color: 'var(--text-muted)' }}>
                        {log.targetUserId ? `User #${log.targetUserId}` : '—'}
                      </td>
                      <td style={{ padding: '0.85rem 1rem' }}>
                        <span
                          className={`badge ${
                            log.action.includes('DENIED') || log.action.includes('BLOCKED')
                              ? 'badge-danger'
                              : log.action.includes('ADMIN')
                              ? 'badge-primary'
                              : 'badge-neutral'
                          }`}
                          style={{ fontSize: '0.72rem', padding: '0.15rem 0.5rem' }}
                        >
                          {log.action}
                        </span>
                      </td>
                      <td style={{ padding: '0.85rem 1rem', color: 'var(--text-main)', fontSize: '0.82rem', maxWidth: '350px', wordBreak: 'break-word' }}>
                        {log.details}
                      </td>
                      <td style={{ padding: '0.85rem 1rem', color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                        {log.ipAddress || '127.0.0.1'}
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>

          {/* Audit Pagination */}
          {auditLogsData && auditLogsData.totalPages > 1 && (
            <div style={{ display: 'flex', alignItems: 'center', justifyContent: 'space-between', borderTop: '1px solid var(--border-subtle)', paddingTop: '1rem' }}>
              <div style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                Page {auditLogsData.number + 1} of {auditLogsData.totalPages}
              </div>
              <div style={{ display: 'flex', gap: '0.5rem' }}>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  disabled={auditLogsData.first || loadingAudit}
                  onClick={() => setAuditPage((p) => Math.max(0, p - 1))}
                >
                  Previous
                </button>
                <button
                  type="button"
                  className="btn btn-secondary btn-sm"
                  disabled={auditLogsData.last || loadingAudit}
                  onClick={() => setAuditPage((p) => p + 1)}
                >
                  Next
                </button>
              </div>
            </div>
          )}
        </section>
      )}

      {/* MODAL 1: USER DETAILS MODAL */}
      {selectedUser && (
        <div className="modal-backdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div className="card" style={{ maxWidth: '650px', width: '100%', maxHeight: '90vh', overflowY: 'auto', padding: '1.75rem', position: 'relative' }}>
            <button
              type="button"
              id="btn-close-details"
              className="btn btn-secondary btn-sm"
              style={{ position: 'absolute', right: '1.25rem', top: '1.25rem', padding: '0.4rem' }}
              onClick={() => setSelectedUser(null)}
            >
              <X size={16} />
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.65rem', marginBottom: '1.25rem' }}>
              <Users size={22} color="var(--primary)" />
              <h2 style={{ fontSize: '1.25rem', fontWeight: 700, color: '#fff' }}>User Profile Overview</h2>
            </div>

            <div style={{ display: 'flex', flexDirection: 'column', gap: '1.25rem' }}>
              {/* Account Identity Banner */}
              <div style={{ background: 'rgba(255,255,255,0.03)', padding: '1rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)' }}>
                <div style={{ display: 'flex', justifyContent: 'space-between', alignItems: 'flex-start' }}>
                  <div>
                    <h3 style={{ fontSize: '1.1rem', fontWeight: 600, color: '#fff' }}>{selectedUser.name}</h3>
                    <div style={{ color: 'var(--text-muted)', fontSize: '0.85rem' }}>{selectedUser.email}</div>
                    {selectedUser.phone && (
                      <div style={{ color: 'var(--text-dim)', fontSize: '0.8rem', marginTop: '0.2rem' }}>Phone: {selectedUser.phone}</div>
                    )}
                  </div>
                  <div style={{ display: 'flex', gap: '0.5rem' }}>
                    <span className="badge badge-primary">{selectedUser.role}</span>
                    <span className={`badge ${selectedUser.status === 'ACTIVE' ? 'badge-success' : 'badge-danger'}`}>
                      {selectedUser.status}
                    </span>
                  </div>
                </div>

                <div style={{ display: 'grid', gridTemplateColumns: '1fr 1fr', gap: '0.5rem', marginTop: '1rem', paddingTop: '0.75rem', borderTop: '1px solid var(--border-subtle)', fontSize: '0.78rem', color: 'var(--text-dim)' }}>
                  <div>Created: {formatDate(selectedUser.createdAt)}</div>
                  <div>Last Login: {formatDate(selectedUser.lastLoginAt)}</div>
                  <div>User UUID: <span style={{ fontFamily: 'monospace' }}>{selectedUser.uuid}</span></div>
                  <div>Enrolled Devices: {selectedUser.deviceCount}</div>
                </div>
              </div>

              {/* Family Pairing Info */}
              <div>
                <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.5rem' }}>Family Association</h4>
                {selectedUser.family ? (
                  <div style={{ background: 'rgba(255,255,255,0.02)', padding: '0.85rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)', fontSize: '0.85rem' }}>
                    <div style={{ fontWeight: 600, color: '#fff' }}>{selectedUser.family.familyName}</div>
                    <div style={{ color: 'var(--text-dim)', fontSize: '0.8rem' }}>Family Code: {selectedUser.family.familyCode} | Members: {selectedUser.family.memberCount}</div>
                  </div>
                ) : (
                  <div style={{ color: 'var(--text-dim)', fontSize: '0.82rem', fontStyle: 'italic' }}>Not currently paired in any family unit.</div>
                )}
              </div>

              {/* Enrolled Devices */}
              <div>
                <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.5rem' }}>Enrolled Hardware Devices</h4>
                {selectedUser.devices.length === 0 ? (
                  <div style={{ color: 'var(--text-dim)', fontSize: '0.82rem', fontStyle: 'italic' }}>No hardware devices registered.</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.5rem' }}>
                    {selectedUser.devices.map((dev) => (
                      <div key={dev.id} style={{ background: 'rgba(255,255,255,0.02)', padding: '0.75rem', borderRadius: 'var(--radius-md)', border: '1px solid var(--border-subtle)', display: 'flex', alignItems: 'center', justifyContent: 'space-between' }}>
                        <div>
                          <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.85rem' }}>{dev.deviceName}</div>
                          <small style={{ color: 'var(--text-dim)' }}>{dev.platform} {dev.osVersion || ''} • Last Seen: {formatDate(dev.lastSeenAt)}</small>
                        </div>
                        <span className={`badge ${dev.online ? 'badge-success' : 'badge-neutral'}`} style={{ fontSize: '0.7rem' }}>
                          {dev.online ? 'Online' : 'Offline'}
                        </span>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* Recent Audit Logs for User */}
              <div>
                <h4 style={{ fontSize: '0.9rem', fontWeight: 600, color: 'var(--text-main)', marginBottom: '0.5rem' }}>Recent Security Events</h4>
                {selectedUser.recentAuditLogs.length === 0 ? (
                  <div style={{ color: 'var(--text-dim)', fontSize: '0.82rem', fontStyle: 'italic' }}>No security events on record.</div>
                ) : (
                  <div style={{ display: 'flex', flexDirection: 'column', gap: '0.4rem', maxHeight: '180px', overflowY: 'auto' }}>
                    {selectedUser.recentAuditLogs.map((log) => (
                      <div key={log.id} style={{ padding: '0.5rem 0.75rem', background: 'rgba(0,0,0,0.2)', borderRadius: 'var(--radius-sm)', fontSize: '0.78rem' }}>
                        <div style={{ display: 'flex', justifyContent: 'space-between', color: 'var(--text-dim)' }}>
                          <span style={{ fontWeight: 600, color: 'var(--primary-light, #818cf8)' }}>{log.action}</span>
                          <span>{formatDate(log.timestamp)}</span>
                        </div>
                        <div style={{ color: 'var(--text-muted)', marginTop: '0.2rem' }}>{log.details}</div>
                      </div>
                    ))}
                  </div>
                )}
              </div>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 2: MODIFY USER MODAL */}
      {editingUser && (
        <div className="modal-backdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div className="card" style={{ maxWidth: '480px', width: '100%', padding: '1.75rem', position: 'relative' }}>
            <button
              type="button"
              id="btn-close-edit"
              className="btn btn-secondary btn-sm"
              style={{ position: 'absolute', right: '1.25rem', top: '1.25rem', padding: '0.4rem' }}
              onClick={() => setEditingUser(null)}
            >
              <X size={16} />
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1.25rem' }}>
              <Edit2 size={20} color="var(--primary)" />
              <h2 style={{ fontSize: '1.2rem', fontWeight: 700, color: '#fff' }}>Modify User Account</h2>
            </div>

            {editError && (
              <div className="badge badge-danger" style={{ padding: '0.65rem 1rem', width: '100%', marginBottom: '1rem', borderRadius: 'var(--radius-sm)' }}>
                {editError}
              </div>
            )}

            <form onSubmit={handleSaveEdit} style={{ display: 'flex', flexDirection: 'column', gap: '1rem' }}>
              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                  Full Name
                </label>
                <input
                  type="text"
                  id="input-edit-name"
                  className="input-field"
                  value={editFormData.name}
                  onChange={(e) => setEditFormData({ ...editFormData, name: e.target.value })}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                  Email Address
                </label>
                <input
                  type="email"
                  id="input-edit-email"
                  className="input-field"
                  value={editFormData.email}
                  onChange={(e) => setEditFormData({ ...editFormData, email: e.target.value })}
                  required
                />
              </div>

              <div>
                <label style={{ display: 'block', fontSize: '0.8rem', fontWeight: 600, color: 'var(--text-muted)', marginBottom: '0.35rem' }}>
                  Phone Number (Optional)
                </label>
                <input
                  type="text"
                  id="input-edit-phone"
                  className="input-field"
                  value={editFormData.phone || ''}
                  onChange={(e) => setEditFormData({ ...editFormData, phone: e.target.value })}
                />
              </div>

              <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem', marginTop: '0.5rem' }}>
                <button
                  type="button"
                  id="btn-cancel-edit"
                  className="btn btn-secondary btn-sm"
                  onClick={() => setEditingUser(null)}
                  disabled={editSaving}
                >
                  Cancel
                </button>
                <button
                  type="submit"
                  id="btn-save-edit"
                  className="btn btn-primary btn-sm"
                  disabled={editSaving}
                  style={{ display: 'flex', alignItems: 'center', gap: '0.4rem' }}
                >
                  {editSaving ? <RefreshCw size={14} className="spin" /> : <Save size={14} />}
                  <span>Save Changes</span>
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL 3: ROLE CHANGE MODAL */}
      {roleChangeUser && (
        <div className="modal-backdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div className="card" style={{ maxWidth: '440px', width: '100%', padding: '1.75rem', position: 'relative' }}>
            <button
              type="button"
              id="btn-close-role-modal"
              className="btn btn-secondary btn-sm"
              style={{ position: 'absolute', right: '1.25rem', top: '1.25rem', padding: '0.4rem' }}
              onClick={() => setRoleChangeUser(null)}
            >
              <X size={16} />
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <ShieldAlert size={22} color="var(--primary)" />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#fff' }}>Change Account Role</h2>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
              Modify role for <strong>{roleChangeUser.email}</strong>. This changes available system capabilities and access levels immediately.
            </p>

            {roleError && (
              <div className="badge badge-danger" style={{ padding: '0.65rem 1rem', width: '100%', marginBottom: '1rem', borderRadius: 'var(--radius-sm)' }}>
                {roleError}
              </div>
            )}

            <div style={{ display: 'flex', flexDirection: 'column', gap: '0.65rem', marginBottom: '1.25rem' }}>
              {(['PARENT', 'CHILD', 'ADMIN'] as RoleType[]).map((r) => (
                <label
                  key={r}
                  style={{
                    display: 'flex',
                    alignItems: 'center',
                    gap: '0.75rem',
                    padding: '0.75rem',
                    borderRadius: 'var(--radius-sm)',
                    background: selectedNewRole === r ? 'rgba(99, 102, 241, 0.15)' : 'rgba(255,255,255,0.02)',
                    border: selectedNewRole === r ? '1px solid var(--primary)' : '1px solid var(--border-subtle)',
                    cursor: 'pointer',
                  }}
                >
                  <input
                    type="radio"
                    name="account-role-selection"
                    id={`radio-role-${r}`}
                    value={r}
                    checked={selectedNewRole === r}
                    onChange={() => setSelectedNewRole(r)}
                  />
                  <div>
                    <div style={{ fontWeight: 600, color: '#fff', fontSize: '0.85rem' }}>{r}</div>
                    <small style={{ color: 'var(--text-dim)', fontSize: '0.75rem' }}>
                      {r === 'PARENT' && 'Full dashboard supervision, pairing initiator, alert receiver'}
                      {r === 'CHILD' && 'Enrolled device telemetry emitter, convocation receiver'}
                      {r === 'ADMIN' && 'System-wide administrative directory & user governance'}
                    </small>
                  </div>
                </label>
              ))}
            </div>

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                id="btn-cancel-role"
                className="btn btn-secondary btn-sm"
                onClick={() => setRoleChangeUser(null)}
                disabled={roleSaving}
              >
                Cancel
              </button>
              <button
                type="button"
                id="btn-confirm-role-change"
                className="btn btn-primary btn-sm"
                onClick={handleConfirmRoleChange}
                disabled={roleSaving || selectedNewRole === roleChangeUser.role}
              >
                {roleSaving ? <RefreshCw size={14} className="spin" /> : 'Confirm Role Transition'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 4: STATUS CHANGE CONFIRMATION MODAL */}
      {statusChangeUser && (
        <div className="modal-backdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.7)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div className="card" style={{ maxWidth: '440px', width: '100%', padding: '1.75rem', position: 'relative' }}>
            <button
              type="button"
              id="btn-close-status-modal"
              className="btn btn-secondary btn-sm"
              style={{ position: 'absolute', right: '1.25rem', top: '1.25rem', padding: '0.4rem' }}
              onClick={() => setStatusChangeUser(null)}
            >
              <X size={16} />
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              {targetNewStatus === 'DISABLED' ? (
                <UserX size={22} color="var(--danger)" />
              ) : (
                <UserCheck size={22} color="#10b981" />
              )}
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#fff' }}>
                {targetNewStatus === 'DISABLED' ? 'Disable Account' : 'Reactivate Account'}
              </h2>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
              Are you sure you want to transition <strong>{statusChangeUser.email}</strong> to status{' '}
              <strong style={{ color: targetNewStatus === 'DISABLED' ? 'var(--danger)' : '#10b981' }}>{targetNewStatus}</strong>?
              {targetNewStatus === 'DISABLED' && (
                <span style={{ display: 'block', marginTop: '0.5rem', color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                  Notice: All active refresh tokens will be revoked immediately and login access will be blocked.
                </span>
              )}
            </p>

            {statusError && (
              <div className="badge badge-danger" style={{ padding: '0.65rem 1rem', width: '100%', marginBottom: '1rem', borderRadius: 'var(--radius-sm)' }}>
                {statusError}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                id="btn-cancel-status"
                className="btn btn-secondary btn-sm"
                onClick={() => setStatusChangeUser(null)}
                disabled={statusSaving}
              >
                Cancel
              </button>
              <button
                type="button"
                id="btn-confirm-status-change"
                className={`btn btn-sm ${targetNewStatus === 'DISABLED' ? 'btn-danger' : 'btn-primary'}`}
                onClick={handleConfirmStatusChange}
                disabled={statusSaving}
              >
                {statusSaving ? <RefreshCw size={14} className="spin" /> : `Confirm ${targetNewStatus}`}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL 5: PERMANENT USER DELETE CONFIRMATION MODAL */}
      {deleteConfirmUser && (
        <div className="modal-backdrop" style={{ position: 'fixed', inset: 0, background: 'rgba(0,0,0,0.75)', zIndex: 1000, display: 'flex', alignItems: 'center', justifyContent: 'center', padding: '1rem' }}>
          <div className="card" style={{ maxWidth: '480px', width: '100%', padding: '1.75rem', position: 'relative', border: '1px solid rgba(239, 68, 68, 0.4)' }}>
            <button
              type="button"
              id="btn-close-delete-modal"
              className="btn btn-secondary btn-sm"
              style={{ position: 'absolute', right: '1.25rem', top: '1.25rem', padding: '0.4rem' }}
              onClick={() => setDeleteConfirmUser(null)}
            >
              <X size={16} />
            </button>

            <div style={{ display: 'flex', alignItems: 'center', gap: '0.5rem', marginBottom: '1rem' }}>
              <Trash2 size={24} color="#ef4444" />
              <h2 style={{ fontSize: '1.15rem', fontWeight: 700, color: '#fff' }}>
                Permanently Delete User Account
              </h2>
            </div>

            <div style={{ background: 'rgba(239, 68, 68, 0.1)', border: '1px solid rgba(239, 68, 68, 0.25)', borderRadius: 'var(--radius-sm)', padding: '0.85rem', marginBottom: '1rem' }}>
              <p style={{ color: '#fca5a5', fontSize: '0.875rem', margin: 0, fontWeight: 500 }}>
                This permanently deletes the account and its associated data. The action cannot be undone.
              </p>
            </div>

            <p style={{ color: 'var(--text-muted)', fontSize: '0.85rem', marginBottom: '1rem' }}>
              Are you sure you want to permanently delete <strong>{deleteConfirmUser.name}</strong> ({deleteConfirmUser.email}, role: <strong>{deleteConfirmUser.role}</strong>)?
              <span style={{ display: 'block', marginTop: '0.5rem', color: 'var(--text-dim)', fontSize: '0.8rem' }}>
                Notice: All active sessions, refresh tokens, enrolled devices, pairing relationships, and approval records will be removed. The email address will immediately become available for fresh registration.
              </span>
            </p>

            {deleteError && (
              <div className="badge badge-danger" style={{ padding: '0.65rem 1rem', width: '100%', marginBottom: '1rem', borderRadius: 'var(--radius-sm)' }}>
                {deleteError}
              </div>
            )}

            <div style={{ display: 'flex', justifyContent: 'flex-end', gap: '0.75rem' }}>
              <button
                type="button"
                id="btn-cancel-delete-user"
                className="btn btn-secondary btn-sm"
                onClick={() => setDeleteConfirmUser(null)}
                disabled={deleteSaving}
              >
                Cancel
              </button>
              <button
                type="button"
                id="btn-confirm-delete-user"
                className="btn btn-danger btn-sm"
                onClick={handleConfirmDelete}
                disabled={deleteSaving}
              >
                {deleteSaving ? <RefreshCw size={14} className="spin" /> : 'Permanently Delete Account'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default AdminDashboardPage;
