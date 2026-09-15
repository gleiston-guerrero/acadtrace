from types import SimpleNamespace
from unittest.mock import MagicMock, patch

import grpc

from docentes.grpc_clients import principal_client


@patch("docentes.grpc_clients.principal_client.contexto_docente_pb2_grpc.TeacherContextServiceStub")
@patch("docentes.grpc_clients.principal_client.grpc.insecure_channel")
def test_get_context_stub_uses_configured_host(channel, stub_class):
    stub = principal_client.get_context_stub()

    channel.assert_called_once_with(principal_client.SGA_PRINCIPAL_HOST)
    stub_class.assert_called_once_with(channel.return_value)
    assert stub is stub_class.return_value


@patch("docentes.grpc_clients.principal_client.get_context_stub")
def test_validate_teacher_assignment_maps_response(stub_factory):
    stub_factory.return_value.ValidateTeacherAssignment.return_value = SimpleNamespace(
        is_valid=True,
        id_asignatura=2,
        id_grado=3,
        id_paralelo=4,
        id_ano_lectivo=5,
        is_active=True,
    )

    result = principal_client.validate_teacher_assignment(7, 11)

    assert result == {
        "is_valid": True,
        "id_asignatura": 2,
        "id_grado": 3,
        "id_paralelo": 4,
        "id_ano_lectivo": 5,
        "is_active": True,
    }
    call = stub_factory.return_value.ValidateTeacherAssignment.call_args
    assert call.args[0].id_docente == 7
    assert call.args[0].id_asignacion == 11
    assert call.kwargs["metadata"] == principal_client.INTERNAL_MD


@patch("docentes.grpc_clients.principal_client.get_context_stub")
def test_validate_teacher_assignment_returns_none_on_grpc_error(stub_factory):
    stub_factory.return_value.ValidateTeacherAssignment.side_effect = grpc.RpcError("failure")

    with patch("builtins.print") as output:
        assert principal_client.validate_teacher_assignment(7, 11) is None

    output.assert_called_once()


@patch("docentes.grpc_clients.principal_client.get_context_stub")
def test_validate_student_enrollment_maps_response_and_error(stub_factory):
    stub = stub_factory.return_value
    stub.ValidateStudentEnrollment.return_value = SimpleNamespace(
        is_valid=True,
        id_estudiante=21,
    )

    assert principal_client.validate_student_enrollment(31, 41) == {
        "is_valid": True,
        "id_estudiante": 21,
    }
    call = stub.ValidateStudentEnrollment.call_args
    assert call.args[0].id_matricula == 31
    assert call.args[0].id_asignacion == 41
    assert call.kwargs["metadata"] == principal_client.INTERNAL_MD

    stub.ValidateStudentEnrollment.side_effect = grpc.RpcError("failure")
    with patch("builtins.print"):
        assert principal_client.validate_student_enrollment(31, 41) is None


@patch("docentes.grpc_clients.principal_client.get_context_stub")
def test_get_current_academic_year_maps_response_and_error(stub_factory):
    stub = stub_factory.return_value
    stub.GetCurrentAcademicYear.return_value = SimpleNamespace(
        id_ano_lectivo=5,
        nombre="2026-2027",
        fecha_inicio="2026-05-01",
        fecha_fin="2027-02-28",
    )

    assert principal_client.get_current_academic_year() == {
        "id_ano_lectivo": 5,
        "nombre": "2026-2027",
        "fecha_inicio": "2026-05-01",
        "fecha_fin": "2027-02-28",
    }
    assert stub.GetCurrentAcademicYear.call_args.kwargs["metadata"] == principal_client.INTERNAL_MD

    stub.GetCurrentAcademicYear.side_effect = grpc.RpcError("failure")
    with patch("builtins.print"):
        assert principal_client.get_current_academic_year() is None


@patch("docentes.grpc_clients.principal_client.get_context_stub")
def test_get_students_by_assignment_maps_response_and_error(stub_factory):
    stub = stub_factory.return_value
    student = SimpleNamespace(
        id_estudiante=21,
        cedula="0000000000",
        nombres="Ana",
        apellidos="Paz",
        id_matricula=31,
    )
    stub.GetStudentsByAssignment.return_value = SimpleNamespace(students=[student])

    assert principal_client.get_students_by_assignment(41) == [
        {
            "id_estudiante": 21,
            "cedula": "0000000000",
            "nombres": "Ana",
            "apellidos": "Paz",
            "id_matricula": 31,
        }
    ]
    call = stub.GetStudentsByAssignment.call_args
    assert call.args[0].id_asignacion == 41
    assert call.kwargs["metadata"] == principal_client.INTERNAL_MD

    stub.GetStudentsByAssignment.side_effect = grpc.RpcError("failure")
    with patch("builtins.print"):
        assert principal_client.get_students_by_assignment(41) == []
