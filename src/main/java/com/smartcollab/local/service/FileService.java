package com.smartcollab.local.service;

import com.smartcollab.local.dto.CopyRequestDto;
import com.smartcollab.local.dto.FileResponseDto;
import com.smartcollab.local.dto.FileSearchResultDto;
import com.smartcollab.local.dto.VersionHistoryDto;
import com.smartcollab.local.entity.*;
import com.smartcollab.local.repository.*;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileService {

    @Value("${file.upload-dir}")
    private String uploadDir;
    private Path rootLocation;
    private Path versionLocation;

    private final FileRepository fileRepository;
    private final UserRepository userRepository;
    private final FolderRepository folderRepository;
    private final TeamRepository teamRepository;
    private final FileVersionRepository fileVersionRepository;
    private final SignatureRepository signatureRepository;
    private final ShareLinkRepository shareLinkRepository;
    private final SignatureService signatureService;

    @PostConstruct
    public void init() {
        try {
            rootLocation = Paths.get(uploadDir);
            versionLocation = rootLocation.resolve("versions");
            Files.createDirectories(rootLocation);
            Files.createDirectories(versionLocation);
        } catch (IOException e) {
            throw new RuntimeException("Could not initialize storage directories", e);
        }
    }

    @Transactional
    public FileEntity uploadFile(MultipartFile file, String username, Long folderId, Long teamId) throws IOException {
        User owner = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder parentFolder;
        if (teamId != null) {
            Team team = teamRepository.findById(teamId)
                    .orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
            if (folderId != null) {
                parentFolder = folderRepository.findById(folderId)
                        .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));
                if (parentFolder.getTeam() == null || !parentFolder.getTeam().getTeamId().equals(teamId)) {
                    throw new SecurityException("선택한 폴더가 해당 팀에 속해있지 않습니다.");
                }
            } else {
                parentFolder = folderRepository.findByTeamAndParentFolderIsNull(team).stream().findFirst()
                        .orElseThrow(() -> new IllegalStateException("팀의 루트 폴더를 찾을 수 없습니다."));
            }
        } else if (folderId != null) {
            parentFolder = folderRepository.findById(folderId)
                    .orElseThrow(() -> new IllegalArgumentException("폴더를 찾을 수 없습니다."));
            if (parentFolder.getTeam() != null) {
                throw new SecurityException("개인 스토리지의 폴더가 아닙니다.");
            }
        } else {
            throw new IllegalArgumentException("업로드할 위치(폴더 또는 팀)가 지정되지 않았습니다.");
        }
        checkFolderPermission(parentFolder, owner);

        String originalName = file.getOriginalFilename();
        String storedName = UUID.randomUUID().toString() + "_" + originalName;
        Path destinationFile = this.rootLocation.resolve(storedName).normalize().toAbsolutePath();

        try (InputStream inputStream = file.getInputStream()) {
            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
        }

        FileEntity fileEntity = FileEntity.builder()
                .owner(owner)
                .folder(parentFolder)
                .originalName(originalName)
                .storedName(storedName)
                .size(file.getSize())
                .build();
        FileEntity savedFile = fileRepository.save(fileEntity);

        FileVersion initialVersion = FileVersion.builder()
                .file(savedFile)
                .storedPath(savedFile.getStoredName())
                .editor(owner)
                .build();
        fileVersionRepository.save(initialVersion);

        // 생성된 초기 버전을 파일의 '활성 버전'으로 지정
        savedFile.setActiveVersion(initialVersion);

        return savedFile;
    }

    @Transactional(readOnly = true)
    public String getLatestFileContent(Long fileId) throws IOException {
        // '가장 최신' 버전이 아닌 '활성' 버전의 내용을 가져옴
        FileEntity file = fileRepository.findByIdWithActiveVersion(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        FileVersion activeVersion = file.getActiveVersion();
        if (activeVersion == null) {
            // 활성 버전이 없는 경우 (데이터 마이그레이션 등 예외 상황)
            // 최초 원본 파일의 내용을 반환하거나 빈 문자열을 반환
            Optional<FileVersion> initialVersion = fileVersionRepository.findTopByFileOrderByVersionIdAsc(file);
            if (initialVersion.isPresent()) {
                activeVersion = initialVersion.get();
            } else {
                return ""; // 버전 정보가 전혀 없는 경우
            }
        }

        String storedPath = activeVersion.getStoredPath();
        Path filePath;
        if (Files.exists(versionLocation.resolve(storedPath))) {
            filePath = versionLocation.resolve(storedPath);
        } else {
            filePath = rootLocation.resolve(storedPath);
        }

        if (!Files.exists(filePath)) {
            return "활성 버전(" + activeVersion.getVersionId() + ")의 실제 파일을 찾을 수 없습니다.";
        }
        return Files.readString(filePath, StandardCharsets.UTF_8);
    }

    @Transactional
    public void saveNewVersion(Long fileId, String content, String username) throws IOException {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FileEntity file = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        checkFileEditPermission(file, user);
        signatureService.invalidateSignatures(file);

        String versionStoredName = UUID.randomUUID().toString() + ".txt";
        Path destinationFile = this.versionLocation.resolve(versionStoredName).normalize().toAbsolutePath();
        Files.writeString(destinationFile, content, StandardCharsets.UTF_8);

        FileVersion fileVersion = FileVersion.builder()
                .file(file)
                .storedPath(versionStoredName)
                .editor(user)
                .build();
        fileVersionRepository.save(fileVersion);

        // 새로 저장된 버전을 파일의 '활성 버전'으로 지정
        file.setActiveVersion(fileVersion);
        file.setSize(Files.size(destinationFile));
        fileRepository.save(file);
    }

    @Transactional(readOnly = true)
    public Resource downloadFile(Long fileId) {

        // 다운로드 시에도 '활성' 버전의 파일을 제공
        FileEntity file = fileRepository.findByIdWithActiveVersion(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다: " + fileId));

        FileVersion activeVersion = file.getActiveVersion();
        if (activeVersion == null) {
            Optional<FileVersion> initialVersion = fileVersionRepository.findTopByFileOrderByVersionIdAsc(file);
            if (initialVersion.isPresent()) {
                activeVersion = initialVersion.get();
            } else {
                throw new RuntimeException("다운로드할 수 있는 파일 버전이 없습니다.");
            }
        }

        Path filePath;
        String storedPath = activeVersion.getStoredPath();
        if (Files.exists(versionLocation.resolve(storedPath))) {
            filePath = versionLocation.resolve(storedPath).normalize();
        } else {
            filePath = rootLocation.resolve(storedPath).normalize();
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() || resource.isReadable()) {
                return resource;
            } else {
                throw new RuntimeException("파일을 읽을 수 없습니다: " + file.getOriginalName());
            }
        } catch (MalformedURLException e) {
            throw new RuntimeException("파일 경로가 올바르지 않습니다: " + file.getOriginalName(), e);
        }
    }

    @Transactional
    public void restoreVersion(Long fileId, Long versionId, String username) throws IOException {

        // 새로운 '활성 버전' 방식의 복원 로직
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));

        checkFileEditPermission(file, user);

        FileVersion versionToRestore = fileVersionRepository.findById(versionId)
                .orElseThrow(() -> new RuntimeException("복원할 버전을 찾을 수 없습니다."));

        // 단순히 파일의 활성 버전을 선택한 버전으로 변경
        file.setActiveVersion(versionToRestore);

        // 복원된 버전의 파일 크기로 업데이트
        String storedPath = versionToRestore.getStoredPath();
        Path filePath;
        if (Files.exists(versionLocation.resolve(storedPath))) {
            filePath = versionLocation.resolve(storedPath);
        } else {
            filePath = rootLocation.resolve(storedPath);
        }
        file.setSize(Files.size(filePath));

        fileRepository.save(file);

        // 활성 내용이 변경되었으므로 서명을 무효화
        signatureService.invalidateSignatures(file);

    }

    @Transactional(readOnly = true)
    public List<VersionHistoryDto> getVersionHistory(Long fileId) {
        FileEntity file = fileRepository.findById(fileId)
                .orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        List<FileVersion> versions = file.getVersions();
        List<Signature> signatures = signatureRepository.findByFile(file);
        return versions.stream()
                .map(version -> new VersionHistoryDto(version, signatures))
                .collect(Collectors.toList());
    }

    @Transactional
    public void renameFile(Long fileId, String newName, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        checkFileEditPermission(fileEntity, user);
        fileEntity.changeName(newName);
        fileRepository.save(fileEntity);
    }

    @Transactional
    public void moveFileToTrash(Long fileId, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        checkFileDeletePermission(fileEntity, user);
        fileEntity.moveToTrash();
        fileRepository.save(fileEntity);
    }

    @Transactional(readOnly = true)
    public List<FileResponseDto> getFilesInTrash(String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        return fileRepository.findByOwnerAndIsDeletedTrue(user).stream()
                .map(FileResponseDto::new)
                .collect(Collectors.toList());
    }

    @Transactional
    public void restoreFileFromTrash(Long fileId, String username) {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        if (!fileEntity.getOwner().getUserId().equals(user.getUserId())) {
            throw new SecurityException("파일 복원 권한이 없습니다.");
        }
        fileEntity.restoreFromTrash();
        fileRepository.save(fileEntity);
    }

    @Transactional
    public void deleteFilePermanently(Long fileId, String username) throws IOException {
        FileEntity fileEntity = fileRepository.findById(fileId).orElseThrow(() -> new RuntimeException("파일을 찾을 수 없습니다."));
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        boolean hasPermission = false;
        if (fileEntity.getOwner().getUserId().equals(user.getUserId())) {
            hasPermission = true;
        }
        if (!hasPermission && fileEntity.getFolder().getTeam() != null) {
            if (fileEntity.getFolder().getTeam().getOwner().getUserId().equals(user.getUserId())) {
                hasPermission = true;
            }
        }
        if (!hasPermission) {
            throw new SecurityException("파일 영구 삭제 권한이 없습니다.");
        }

        List<ShareLink> shareLinks = shareLinkRepository.findByFile(fileEntity);
        shareLinkRepository.deleteAll(shareLinks);

        for(FileVersion version : fileEntity.getVersions()) {
            Path versionFile = versionLocation.resolve(version.getStoredPath());
            Files.deleteIfExists(versionFile);
        }

        Path fileToDelete = rootLocation.resolve(fileEntity.getStoredName());
        Files.deleteIfExists(fileToDelete);
        fileRepository.delete(fileEntity);
    }

    private void checkFolderPermission(Folder folder, User user) {
        boolean hasPermission = false;
        if (folder.getOwner().getUserId().equals(user.getUserId())) {
            hasPermission = true;
        } else if (folder.getTeam() != null) {
            hasPermission = folder.getTeam().getTeamMembers().stream()
                    .anyMatch(member -> member.getUser().getUserId().equals(user.getUserId()) && member.isCanEdit());
        }

        if (!hasPermission) {
            throw new SecurityException("폴더에 접근하거나 파일을 업로드할 권한이 없습니다.");
        }
    }

    private void checkFileEditPermission(FileEntity file, User user) {
        boolean hasPermission = false;
        if (file.getOwner().getUserId().equals(user.getUserId())) {
            hasPermission = true;
        } else if (file.getFolder().getTeam() != null) {
            hasPermission = file.getFolder().getTeam().getTeamMembers().stream()
                    .anyMatch(m -> m.getUser().getUserId().equals(user.getUserId()) && m.isCanEdit());
        }
        if (!hasPermission) {
            throw new SecurityException("파일 수정 권한이 없습니다.");
        }
    }

    private void checkFileDeletePermission(FileEntity file, User user) {
        boolean hasPermission = false;
        if (file.getOwner().getUserId().equals(user.getUserId())) {
            hasPermission = true;
        } else if (file.getFolder().getTeam() != null) {
            hasPermission = file.getFolder().getTeam().getTeamMembers().stream()
                    .anyMatch(m -> m.getUser().getUserId().equals(user.getUserId()) && m.isCanDelete());
        }
        if (!hasPermission) {
            throw new SecurityException("파일 삭제 권한이 없습니다.");
        }
    }

    @Transactional
    public void moveItems(List<String> itemUniqueKeys, Long destinationFolderId, String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder destination = folderRepository.findById(destinationFolderId)
                .orElseThrow(() -> new IllegalArgumentException("이동할 대상 폴더를 찾을 수 없습니다."));
        checkFolderPermission(destination, user);

        List<Folder> foldersToMove = new ArrayList<>();
        List<FileEntity> filesToMove = new ArrayList<>();

        for (String key : itemUniqueKeys) {
            String[] parts = key.split("-");
            String type = parts[0];
            Long id = Long.parseLong(parts[1]);
            if ("folder".equals(type)) {
                Folder folder = folderRepository.findById(id).orElseThrow();
                if (destination.getFolderId().equals(folder.getFolderId())) {
                    throw new IllegalArgumentException("폴더를 자기 자신에게 이동할 수 없습니다.");
                }
                foldersToMove.add(folder);
            } else {
                filesToMove.add(fileRepository.findById(id).orElseThrow());
            }
        }

        for (Folder folder : foldersToMove) {
            folder.setParentFolder(destination);
            folderRepository.save(folder);
        }

        for (FileEntity file : filesToMove) {
            file.setFolder(destination);
            fileRepository.save(file);
        }
    }

    @Transactional
    public void copyFiles(CopyRequestDto request, String username) throws IOException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        Folder destination = folderRepository.findById(request.getDestinationFolderId())
                .orElseThrow(() -> new IllegalArgumentException("복사할 대상 폴더를 찾을 수 없습니다."));
        checkFolderPermission(destination, user);

        for (String key : request.getItemUniqueKeys()) {
            String[] parts = key.split("-");
            String type = parts[0];
            Long id = Long.parseLong(parts[1]);

            if ("folder".equals(type)) {
                throw new IllegalArgumentException("폴더 복사는 현재 지원되지 않습니다. 파일만 선택해주세요.");
            } else {
                FileEntity originalFile = fileRepository.findById(id)
                        .orElseThrow(() -> new RuntimeException("원본 파일을 찾을 수 없습니다."));

                String newStoredName = UUID.randomUUID().toString() + "_" + originalFile.getOriginalName();
                Path sourcePath = this.rootLocation.resolve(originalFile.getStoredName());
                Path destinationPath = this.rootLocation.resolve(newStoredName);
                Files.copy(sourcePath, destinationPath, StandardCopyOption.REPLACE_EXISTING);

                FileEntity copiedFile = FileEntity.builder()
                        .owner(user)
                        .folder(destination)
                        .originalName(originalFile.getOriginalName())
                        .storedName(newStoredName)
                        .size(originalFile.getSize())
                        .build();
                fileRepository.save(copiedFile);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<FileSearchResultDto> searchFiles(String query, Long teamId, String username) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));
        List<Folder> rootFolders;
        if (teamId != null) {
            Team team = teamRepository.findById(teamId).orElseThrow(() -> new IllegalArgumentException("팀을 찾을 수 없습니다."));
            rootFolders = folderRepository.findByTeamAndParentFolderIsNull(team);
        } else {
            rootFolders = folderRepository.findByOwnerAndTeamIsNullAndParentFolderIsNull(user);
        }

        List<FileSearchResultDto> results = new ArrayList<>();
        for (Folder root : rootFolders) {
            findFilesRecursive(root, query, "/" + root.getName(), results);
        }
        return results;
    }

    private void findFilesRecursive(Folder currentFolder, String query, String currentPath, List<FileSearchResultDto> results) {
        // 삭제된 파일을 제외하고 검색하는 새 메서드를 호출하도록 변경
        fileRepository.findByFolderAndOriginalNameContainingIgnoreCaseAndIsDeletedFalse(currentFolder, query)
                .stream()
                .map(file -> new FileSearchResultDto(file, currentPath))
                .forEach(results::add);

        for (Folder subFolder : currentFolder.getSubFolders()) {
            findFilesRecursive(subFolder, query, currentPath + "/" + subFolder.getName(), results);
        }
    }

}