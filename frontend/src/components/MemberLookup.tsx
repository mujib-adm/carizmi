import { SearchOutlined, UserOutlined } from '@ant-design/icons';
import { ConfigProvider, Empty, Select, Spin, Tag } from 'antd';
import { useEffect, useRef, useState } from 'react';
import { membersApi } from '../api/generated/members/members';
import type { MemberLookupDto } from '../api/generated/types/index';

import type { SelectProps } from 'antd';
import { ReferenceConstants } from '../constants/ReferenceConstants';
import { useReference } from '../hooks/useReference';
import '../styles/components/MemberLookup.css';

interface MemberLookupProps extends SelectProps<number> {
  onSelectMember?: (member: MemberLookupDto | null) => void;
  onError?: (error: any) => void;
  compact?: boolean;
}

export default function MemberLookup({
  value,
  onChange,
  onSelectMember,
  onError,
  compact = false,
  className,
  style,
  ...rest
}: MemberLookupProps) {
  const { toDisplay } = useReference();
  const [data, setData] = useState<MemberLookupDto[]>([]);
  const [fetching, setFetching] = useState(false);

  const timeoutRef = useRef<ReturnType<typeof setTimeout> | null>(null);

  const handleSearch = async (newValue: string) => {
    if (!newValue || newValue.length < 3) {
      setData([]);
      return;
    }
    setFetching(true);
    try {
      const result = await membersApi.lookupMembers({ query: newValue });
      if (result && result.responseData) {
        setData(result.responseData);
      } else {
        setData([]);
      }
    } catch (error) {
      if (onError) onError(error);
    } finally {
      setFetching(false);
    }
  };

  useEffect(() => {
    return () => {
      if (timeoutRef.current) clearTimeout(timeoutRef.current);
    };
  }, []);

  const debouncedSearch = (val: string) => {
    if (timeoutRef.current) clearTimeout(timeoutRef.current);
    timeoutRef.current = setTimeout(() => {
      handleSearch(val);
    }, 300);
  };

  const handleChange = (newValue: number, option: any) => {
    onChange?.(newValue);
    if (onSelectMember) {
      onSelectMember(option?.member ?? null);
    }
  };

  return (
    <ConfigProvider
      theme={{
        components: {
          Select: {
            controlHeight: compact ? 32 : 58,
            fontSize: compact ? 13 : 14,
            optionPadding: compact ? '6px 12px' : '12px 12px',
          },
        },
      }}
    >
      <div
        className={`member-lookup-wrapper ${compact ? 'member-lookup-compact' : ''}`}
        style={compact ? { width: 'auto', display: 'inline-block' } : undefined}
      >
        <SearchOutlined
          className={`member-lookup-prefix-icon ${compact ? 'member-lookup-prefix-icon-compact' : ''}`}
        />
        <Select
          className={`member-lookup-select ${compact ? 'member-lookup-select-compact' : ''} ${className || ''}`}
          style={{ width: compact ? 220 : '100%', ...style }}
          popupMatchSelectWidth={compact ? false : undefined}
          styles={compact ? { popup: { root: { minWidth: 280 } } } : undefined}
          {...rest}
          showSearch
          value={value}
          placeholder={
            <div className="member-lookup-placeholder">Search Member...</div>
          }
          defaultActiveFirstOption={false}
          filterOption={false}
          onSearch={debouncedSearch}
          onChange={handleChange}
          notFoundContent={
            fetching ? (
              <Spin size="small" />
            ) : (
              <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="Type 3+ chars" />
            )
          }
          suffixIcon={<UserOutlined className="member-lookup-suffix-icon" />}
          options={(data || []).map((d: MemberLookupDto) => {
            const isInactive = d.status === ReferenceConstants.MEMBER_STATUS.INACTIVE;
            const isPending = d.status === ReferenceConstants.MEMBER_STATUS.PENDING;
            const statusLabel = isInactive
              ? toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, d.status) || 'Inactive'
              : isPending
              ? toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, d.status) || 'Pending'
              : '';
            return {
              value: d.memberID,
              label: `${d.firstName} ${d.lastName}${statusLabel ? ` (${statusLabel})` : ''}`,
              member: d,
            };
          })}
          optionRender={(option) => {
            const d = option.data.member;
            const isActive = d.status === ReferenceConstants.MEMBER_STATUS.ACTIVE;
            const isPending = d.status === ReferenceConstants.MEMBER_STATUS.PENDING;
            const isInactive = d.status === ReferenceConstants.MEMBER_STATUS.INACTIVE;
            const activeLabel = toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, d.status) || 'Active';
            const pendingLabel = toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, d.status) || 'Pending';
            const inactiveLabel = toDisplay(ReferenceConstants.MEMBER_STATUS.NAME, d.status) || 'Inactive';

            if (compact) {
              return (
                <div className="member-option-container member-option-container-compact">
                  <div className="member-option-info member-option-info-compact">
                    <span className="member-name">
                      {d.firstName} {d.lastName}
                    </span>
                    {d.phone && (
                      <span className="member-details">
                        Phone: {d.phone}
                      </span>
                    )}
                  </div>
                  {isActive ? (
                    <Tag className="member-tag member-tag-compact member-tag-active" color="success">
                      {activeLabel}
                    </Tag>
                  ) : isPending ? (
                    <Tag className="member-tag member-tag-compact member-tag-pending" color="warning">
                      {pendingLabel}
                    </Tag>
                  ) : isInactive ? (
                    <Tag className="member-tag member-tag-compact member-tag-inactive" color="error">
                      {inactiveLabel}
                    </Tag>
                  ) : null}
                </div>
              );
            }
            return (
              <div className="member-option-container">
                <div className="member-option-info">
                  <span className="member-name">
                    {d.firstName} {d.lastName}
                  </span>
                  <span className="member-details">
                    ID: <span className="member-id-mono">{d.memberID}</span> • Phone: {d.phone}
                  </span>
                </div>
                {isActive ? (
                  <Tag className="member-tag member-tag-active" color="success">
                    {activeLabel}
                  </Tag>
                ) : isPending ? (
                  <Tag className="member-tag member-tag-pending" color="warning">
                    {pendingLabel}
                  </Tag>
                ) : isInactive ? (
                  <Tag className="member-tag member-tag-inactive" color="error">
                    {inactiveLabel}
                  </Tag>
                ) : null}
              </div>
            );
          }}
        />
      </div>
    </ConfigProvider>
  );
}