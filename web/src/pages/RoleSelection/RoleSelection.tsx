import React, { useState } from 'react';
import './RoleSelection.css';

export type RoleType = 'PARENT' | 'CHILD';

interface RoleSelectionProps {
  initialRole?: RoleType;
  onRoleConfirmed?: (role: RoleType) => void;
}

export const RoleSelection: React.FC<RoleSelectionProps> = ({
  initialRole,
  onRoleConfirmed
}) => {
  const [selectedRole, setSelectedRole] = useState<RoleType | null>(initialRole || null);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  const handleConfirm = async () => {
    if (!selectedRole) return;

    setIsSubmitting(true);
    setErrorMessage(null);

    try {
      // In web app, calls POST /api/v1/role/select with Authorization Bearer
      const token = localStorage.getItem('accessToken');
      if (token) {
        const response = await fetch('/api/v1/role/select', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'Authorization': `Bearer ${token}`
          },
          body: JSON.stringify({ role: selectedRole })
        });

        if (!response.ok) {
          throw new Error('Failed to update role on server');
        }

        const data = await response.json();
        if (data.data?.accessToken) {
          localStorage.setItem('accessToken', data.data.accessToken);
        }
      }

      localStorage.setItem('userRole', selectedRole);

      if (onRoleConfirmed) {
        onRoleConfirmed(selectedRole);
      } else {
        // Default navigation to connection screen
        window.location.href = `/connection?role=${selectedRole}`;
      }
    } catch (err: any) {
      setErrorMessage(err.message || 'An error occurred while confirming role');
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <div className="role-selection-container">
      <div className="role-selection-card">
        <header className="role-header">
          <h1 className="role-logo">Nivya</h1>
          <h2 className="role-title">Select Your Role</h2>
          <p className="role-subtitle">
            Your role determines which features and interface you access.
            The connection code screen will follow.
          </p>
        </header>

        {errorMessage && (
          <div style={{ color: '#ef4444', marginBottom: '16px', textAlign: 'center', fontSize: '14px' }}>
            {errorMessage}
          </div>
        )}

        <div className="role-cards-grid">
          {/* Parent Role Card */}
          <div
            className={`role-option-card ${selectedRole === 'PARENT' ? 'selected' : ''}`}
            onClick={() => setSelectedRole('PARENT')}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && setSelectedRole('PARENT')}
          >
            <div className="role-card-header">
              <h3 className="role-name">Parent</h3>
              <div className="role-radio">
                <div className="role-radio-dot" />
              </div>
            </div>
            <div className="role-tagline">Family Safety &amp; Device Health</div>
            <p className="role-desc">
              Monitor phone status, track location, review screen time summaries, and send family guidance.
            </p>
            <ul className="role-features">
              <li className="role-feature-item">
                <span className="role-feature-icon">✓</span> Live device status &amp; battery
              </li>
              <li className="role-feature-item">
                <span className="role-feature-icon">✓</span> Location &amp; 30-day history
              </li>
              <li className="role-feature-item">
                <span className="role-feature-icon">✓</span> Retained Convocation history
              </li>
            </ul>
          </div>

          {/* Child Role Card */}
          <div
            className={`role-option-card ${selectedRole === 'CHILD' ? 'selected' : ''}`}
            onClick={() => setSelectedRole('CHILD')}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => e.key === 'Enter' && setSelectedRole('CHILD')}
          >
            <div className="role-card-header">
              <h3 className="role-name">Child</h3>
              <div className="role-radio">
                <div className="role-radio-dot" />
              </div>
            </div>
            <div className="role-tagline">Personal Device Care</div>
            <p className="role-desc">
              Share health status with parents, monitor your personal screen time, and clean up temporary storage.
            </p>
            <ul className="role-features">
              <li className="role-feature-item">
                <span className="role-feature-icon">✓</span> Battery &amp; network quality indicator
              </li>
              <li className="role-feature-item">
                <span className="role-feature-icon">✓</span> Temporary cache clean-up tool
              </li>
              <li className="role-feature-item">
                <span className="role-feature-icon">✓</span> Private Convocation notepad
              </li>
            </ul>
          </div>
        </div>

        <button
          className="role-action-btn"
          disabled={!selectedRole || isSubmitting}
          onClick={handleConfirm}
        >
          {isSubmitting ? 'Configuring Session...' : 'Continue to Connection'}
        </button>
      </div>
    </div>
  );
};

export default RoleSelection;
