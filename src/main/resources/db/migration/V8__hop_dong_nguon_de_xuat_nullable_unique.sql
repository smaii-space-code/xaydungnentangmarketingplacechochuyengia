-- SQL Server UNIQUE permits only one NULL. Direct-hire contracts have no proposal;
-- preserve uniqueness for actual proposal IDs without creating synthetic sources.
DECLARE @proposalUnique SYSNAME;
SELECT @proposalUnique=kc.name FROM sys.key_constraints kc
JOIN sys.index_columns ic ON ic.object_id=kc.parent_object_id AND ic.index_id=kc.unique_index_id
JOIN sys.columns c ON c.object_id=ic.object_id AND c.column_id=ic.column_id
WHERE kc.parent_object_id=OBJECT_ID('dbo.HopDong') AND kc.type='UQ' AND c.name='MaDeXuat'
AND NOT EXISTS(SELECT 1 FROM sys.index_columns other WHERE other.object_id=ic.object_id AND other.index_id=ic.index_id AND other.key_ordinal>1);
IF @proposalUnique IS NOT NULL
BEGIN
 DECLARE @dropProposalUnique NVARCHAR(500)=N'ALTER TABLE dbo.HopDong DROP CONSTRAINT '+QUOTENAME(@proposalUnique);
 EXEC sp_executesql @dropProposalUnique;
END;
GO
CREATE UNIQUE INDEX UX_HopDong_DeXuat ON HopDong(MaDeXuat) WHERE MaDeXuat IS NOT NULL;
